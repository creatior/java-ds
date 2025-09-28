import mpi.*;

public class Main {
    public static void main(String[] args) {
        MPI.Init(args);

        int myrank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        int TAG = 0;

        int[] buf = new int[1];
        buf[0] = myrank;

        int[] rec_message = new int[1];

        // sync mode
//        if (myrank == 0) {
//            MPI.COMM_WORLD.Sendrecv(buf, 0, 1, MPI.INT, myrank + 1, TAG, rec_message, 0, 1, MPI.INT, size - 1, TAG);
//            buf[0] += rec_message[0];
//            System.out.println("Rank sum from process " + myrank + ": " + buf[0]);
//        } else if (myrank == (size - 1)){
//            MPI.COMM_WORLD.Recv(rec_message, 0, 1, MPI.INT, myrank - 1, TAG);
//            System.out.println("Process " + myrank + " received message: " + rec_message[0]);
//            buf[0] += rec_message[0];
//            MPI.COMM_WORLD.Send(buf, 0, 1, MPI.INT, 0, TAG);
//        } else {
//            MPI.COMM_WORLD.Recv(rec_message, 0, 1, MPI.INT, myrank - 1, TAG);
//            System.out.println("Process " + myrank + " received message: " + rec_message[0]);
//            buf[0] += rec_message[0];
//            MPI.COMM_WORLD.Send(buf, 0, 1, MPI.INT, myrank + 1, TAG);
//        }

        // async mode
        if (myrank == 0) {
            MPI.COMM_WORLD.Isend(buf, 0, 1, MPI.INT, myrank + 1, TAG);
            Request recvReq = MPI.COMM_WORLD.Irecv(rec_message, 0, 1, MPI.INT, size - 1, TAG);
            recvReq.Wait();
            buf[0] += rec_message[0];
            System.out.println("Rank sum from process " + myrank + ": " + buf[0]);

        } else if (myrank == (size - 1)) {
            Request recvReq = MPI.COMM_WORLD.Irecv(rec_message, 0, 1, MPI.INT, myrank - 1, TAG);
            recvReq.Wait();
            System.out.println("Process " + myrank + " received message: " + rec_message[0]);
            buf[0] += rec_message[0];
            MPI.COMM_WORLD.Isend(buf, 0, 1, MPI.INT, 0, TAG);
        } else {
            Request recvReq = MPI.COMM_WORLD.Irecv(rec_message, 0, 1, MPI.INT, myrank - 1, TAG);
            recvReq.Wait();
            System.out.println("Process " + myrank + " received message: " + rec_message[0]);
            buf[0] += rec_message[0];
            MPI.COMM_WORLD.Isend(buf, 0, 1, MPI.INT, myrank + 1, TAG);
        }

        MPI.Finalize();

        }
}