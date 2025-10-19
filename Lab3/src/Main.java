import mpi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Задание 3
        int LEVEL2_BLOCKS = 4;
        long startTime = System.currentTimeMillis();

        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        int TAG = 0;

        if (rank > LEVEL2_BLOCKS) {
            int[] data = new int[]{rank-LEVEL2_BLOCKS-1};
            int dest = (rank % LEVEL2_BLOCKS) + 1;
            MPI.COMM_WORLD.Send(data, 0, 1, MPI.INT, dest, TAG);

        } else if (rank > 0) {
            List<Integer> collected = new ArrayList<>();
            int numSenders = 0;
            for (int i = LEVEL2_BLOCKS + 1; i < size; i++) {
                if ((i % LEVEL2_BLOCKS) + 1 == rank) {
                    numSenders++;
                }
            }

            int[][] bufs = new int[numSenders][1];
            Request[] reqs = new Request[numSenders];
            for (int i = 0; i < numSenders; i++) {
                bufs[i] = new int[1];
                reqs[i] = MPI.COMM_WORLD.Irecv(bufs[i], 0, 1, MPI.INT, MPI.ANY_SOURCE, TAG);
            }
            Request.Waitall(reqs);

            for (int i = 0; i < numSenders; i++) {
                collected.add(bufs[i][0]);
            }
            Collections.sort(collected);

            int[] arr = collected.stream().mapToInt(Integer::intValue).toArray();

            long completeTime = System.currentTimeMillis() - startTime;
            System.out.println("Process with rank " + rank + " finished in " + completeTime + "ms");
            System.out.println("Block " + rank + " result: " + collected);
            MPI.COMM_WORLD.Send(arr, 0, arr.length, MPI.INT, 0, TAG);
        } else {
            long blockStartTime = System.currentTimeMillis();
            List<Integer> finalCollected = new ArrayList<>();
            for (int src = 1; src <= LEVEL2_BLOCKS; src++) {
                Status st = MPI.COMM_WORLD.Probe(src, TAG);
                int count = st.Get_count(MPI.INT);
                int[] rec_buf = new int[count];
                MPI.COMM_WORLD.Recv(rec_buf, 0, count, MPI.INT, src, TAG);
                for (int v : rec_buf) {
                    finalCollected.add(v);
                }
            }
            Collections.sort(finalCollected);
            long completeTime = System.currentTimeMillis() - blockStartTime;
            long programCompleteTime = System.currentTimeMillis() - startTime;
            System.out.println("Process with rank " + rank + " finished in " + completeTime + "ms");
            System.out.println("Program finished in " + programCompleteTime + "ms");
            System.out.println("Final result: " + finalCollected);
        }

        MPI.Finalize();
    }
}