import mpi.*;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int n = 5000;
        int[] graph = new int[n * n];

        if (rank == 0) {
            for (int i = 0; i < n - 1; i++) {
                graph[i * n + (i + 1)] = 1;
                graph[(i + 1) * n + i] = 1;
            }
        }

        MPI.COMM_WORLD.Bcast(graph, 0, n * n, MPI.INT, 0);

        long start = System.nanoTime();

        int rowsPerProc = n / size;
        int extra = n % size;

        int startRow = rank * rowsPerProc + Math.min(rank, extra);
        int localRows = rowsPerProc + (rank < extra ? 1 : 0);
        int endRow = startRow + localRows;

        int localEdges = 0;
        for (int u = startRow; u < endRow; u++) {
            int row = u * n;
            for (int v = u + 1; v < n; v++) {
                if (graph[row + v] != 0) localEdges++;
            }
        }

        int[] totalEdgesArr = new int[1];
        MPI.COMM_WORLD.Reduce(new int[]{localEdges}, 0, totalEdgesArr, 0, 1, MPI.INT, MPI.SUM, 0);
        int totalEdges = totalEdgesArr[0];

        // BFS

        boolean[] localVisited = new boolean[localRows];
        ArrayDeque<Integer> localQueue = new ArrayDeque<>();
        if (rank == 0) {
            localVisited[0] = true; // стартовая вершина 0
            localQueue.add(0);
        }

        boolean active;
        do {
            List<List<Integer>> sendBuffers = new ArrayList<>();
            for (int i = 0; i < size; i++) sendBuffers.add(new ArrayList<>());

            // Обработка локальной очереди
            while (!localQueue.isEmpty()) {
                int u = localQueue.poll();
                int row = u * n;
                for (int v = 0; v < n; v++) {
                    if (graph[row + v] != 0) {
                        int owner = getOwner(v, n, size);
                        if (owner == rank) {
                            int localIdx = v - startRow;
                            if (!localVisited[localIdx]) {
                                localVisited[localIdx] = true;
                                localQueue.add(v);
                            }
                        } else {
                            sendBuffers.get(owner).add(v);
                        }
                    }
                }
            }

            // Обмен вершинами между процессами
            exchangeVertices(sendBuffers, localQueue, rank, size);

            // Проверка, есть ли ещё активные вершины
            int localActive = localQueue.isEmpty() ? 0 : 1;
            int[] globalActive = new int[1];
            MPI.COMM_WORLD.Allreduce(new int[]{localActive}, 0, globalActive, 0, 1, MPI.INT, MPI.SUM);
            active = globalActive[0] > 0;

        } while (active);

// Проверка связности
        boolean connected = true;
        for (boolean b : localVisited) {
            if (!b) { connected = false; break; }
        }
        int[] globalConnected = new int[1];
        MPI.COMM_WORLD.Allreduce(new int[]{connected ? 1 : 0}, 0, globalConnected, 0, 1, MPI.INT, MPI.MIN);
        boolean isTree = (totalEdges == n - 1) && (globalConnected[0] == 1);

        MPI.COMM_WORLD.Barrier();
        if (rank == 0) {
            long timeMs = (System.nanoTime() - start) / 1_000_000;

            try (FileWriter fw = new FileWriter("tree_time.csv", true)) {
                fw.write(size + "," + timeMs + "\n");
            }
        }

        MPI.Finalize();
    }

    // Определяем владельца вершины
    static int getOwner(int v, int n, int size) {
        int rowsPerProc = n / size;
        int extra = n % size;
        for (int rank = 0; rank < size; rank++) {
            int start = rank * rowsPerProc + Math.min(rank, extra);
            int count = rowsPerProc + (rank < extra ? 1 : 0);
            int end = start + count;
            if (v >= start && v < end) return rank;
        }
        return -1; // не должно происходить
    }

    // Обмен вершинами между процессами
// Обмен вершинами между процессами через Allgather
    static void exchangeVertices(List<List<Integer>> sendBuffers, ArrayDeque<Integer> localQueue, int rank, int size) throws MPIException {
        // Преобразуем свои новые вершины в массив
        List<Integer> myNewVertices = sendBuffers.get(rank);
        int sendCount = myNewVertices.size();
        int[] sendArray = new int[sendCount];
        for (int i = 0; i < sendCount; i++) sendArray[i] = myNewVertices.get(i);

        // Сначала собираем количество элементов от всех процессов
        int[] recvCounts = new int[size];
        MPI.COMM_WORLD.Allgather(new int[]{sendCount}, 0, 1, MPI.INT, recvCounts, 0, 1, MPI.INT);

        // Вычисляем смещения для приёма
        int totalRecv = 0;
        int[] displs = new int[size];
        for (int i = 0; i < size; i++) {
            displs[i] = totalRecv;
            totalRecv += recvCounts[i];
        }

        // Общий массив для приёма всех новых вершин
        int[] recvArray = new int[totalRecv];

        // Allgatherv: каждый процесс собирает вершины всех процессов
        MPI.COMM_WORLD.Allgatherv(sendArray, 0, sendCount, MPI.INT,
                recvArray, 0, recvCounts, displs, MPI.INT);

        // Добавляем полученные вершины в локальную очередь
        for (int v : recvArray) {
            if (!localQueue.contains(v)) { // избегаем дубликатов
                localQueue.add(v);
            }
        }
    }
}
