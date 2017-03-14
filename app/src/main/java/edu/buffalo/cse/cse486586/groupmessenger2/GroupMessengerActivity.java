package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    //begin xue *****************************************************************************//
    static final String TAG = GroupMessengerActivity.class.getSimpleName();

//    static final String REMOTE_PORT0 = "11108";
//    static final String REMOTE_PORT1 = "11112";
//    static final String REMOTE_PORT2 = "11116";
//    static final String REMOTE_PORT3 = "11120";
//    static final String REMOTE_PORT4 = "11124";

    static final String[] REMOTE_PORTS = new String[]{"11108","11112","11116","11120","11124"};
//    static final String[] REMOTE_PORTS = new String[]{"11108","11112"};


    static final int SERVER_PORT = 10000;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private ContentResolver contentResolver;
    private Uri uri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    ///end ************************************************************************************///
    ///begin new local variables***************************************************************///

//    private HashMap<String, Socket> groupSockets;

    private String selfMachineName;  //these two can combine to generate the UniqueId of msg, such as 5554_2.
    private HashMap<String, Boolean> machineStatus = new HashMap<String, Boolean>(); //initial with all 1, and 0 if one machine is down.
    private HashMap<String, Integer> fifoSequences = new HashMap<String, Integer>();// <#p1, #p2, #p3, #p4, #p5>
    private Integer maxAgreedSeq = 0;  //maximum observed agreed sequence
    private Integer maxProposedSeq = 0; //maximum proposed sequence by myself


    private LinkedList<Integer[]> fifoQueue = new LinkedList<Integer[]>();
    private LinkedList<TOQueueItem> totalQueue = new LinkedList<TOQueueItem>();

    private Integer counter = 0;
    private Integer siProposedSeq  = 0;





    //end ************************************************************************************///

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        //begin xue *****************************************************************************//

        //assign var
        contentResolver = getContentResolver();
        uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

        //Calculate the port number that this AVD listens on.
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        //initialization
        selfMachineName = myPort;
        for (String machine:REMOTE_PORTS){
            machineStatus.put(machine, null);
            fifoSequences.put(machine, 0);
        }






        // new a ServerTask()
        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        }catch (IOException e){
            Log.e(TAG, "Can't create a ServerSocket");
        }
        ///end *********************************************************************************///

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        //begin xue *****************************************************************************//
        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);

            }
        });
        ///end *********************************************************************************///

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////    ServerTask      /////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////


    /// begin xue **************************//
    public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        HashMap<String, Socket> machineSockets = new HashMap<String, Socket>();

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

            try {
                //test for timeoutException
//                serverSocket.setSoTimeout(10000);

                while(true) {
                    Socket socket_sv = serverSocket.accept();
                    String[] cntnt;
                    String socketRemotePort;

//                    BufferedReader bfr = new BufferedReader(new InputStreamReader(socket_sv.getInputStream()));
//                    String tmp = bfr.readLine();

                    Scanner scanner = new Scanner(socket_sv.getInputStream());
                    String tmp;
                    while(!scanner.hasNext()){;}
                    tmp = scanner.nextLine();
                    cntnt = tmp.split(" ", 3);
                    socketRemotePort = cntnt[1];
                    machineSockets.put(socketRemotePort, socket_sv);

                    BDeliver1(cntnt[0], cntnt[1], cntnt[2]);

                    while(!scanner.hasNext()){;}
                    tmp = scanner.nextLine();
                    cntnt = tmp.split(" ");
                    BDeliver2(cntnt[0], cntnt[1], cntnt[2], cntnt[3]);


                    machineSockets.remove(socketRemotePort);
                    socket_sv.close();
                }
            } catch (SocketTimeoutException e){
                Log.e(TAG, "There is a SocketTimeOutException!"+e.getMessage());

            } catch (Exception e) {
                Log.e(TAG, "error bufferedReader"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String...strings) {

            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strings[1] + "\n");

            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD, strings[0]);
            contentValues.put(VALUE_FIELD, strings[1]);
            contentResolver.insert(uri,contentValues);
        }

        private void BDeliver1(String mid, String jProc, String msg){
            siProposedSeq += 1;
            String toSendStr = mid + " " + siProposedSeq.toString();
            Socket toSendSocket = machineSockets.get(jProc);


            try{
                OutputStream outputStream = toSendSocket.getOutputStream();
                outputStream.write(toSendStr.getBytes());
            } catch (IOException e){
                Log.e(TAG, e.getMessage());
            }

            totalQueue.add(new TOQueueItem(mid, jProc, siProposedSeq.toString(), selfMachineName, false, msg));
            organizeTotalQueue();
        }

        private void BDeliver2(String mid, String iProc, String sk, String kProc){
            try{
                Integer skInteger = Integer.parseInt(sk);
                siProposedSeq = (siProposedSeq > skInteger)? siProposedSeq : skInteger;

                for (TOQueueItem item:totalQueue){
                    if (item.mID.equals(mid) && item.jProcSentMsg.equals(iProc)){
                        //change the proposed sequence number to sk
                        item.sProposedSeq = sk;

                        //change process that suggested sequence number to k
                        item.kProcProposing = kProc;

                        //change undeliverable to deliverable
                        item.status = true;

                        break;
                    }
                }
                organizeTotalQueue();
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }

        private void organizeTotalQueue(){
            Collections.sort(totalQueue, new ComparatorTOQueueItem());
            while (!totalQueue.isEmpty() && totalQueue.peekFirst().status){
                publishProgress(totalQueue.peekFirst().sProposedSeq, totalQueue.peekFirst().msg);
                totalQueue.removeFirst();
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////    ClientTask      /////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    public class ClientTask extends AsyncTask<String, Void, Void> {

        Socket[] socketsSV = new Socket[REMOTE_PORTS.length];
        ArrayList<Pair<String, String>> pairProposedSeqList = new ArrayList<Pair<String, String>>();

        @Override
        protected Void doInBackground(String... msgs) {

            counter += 1;
            socketsSV = BMulticastMSG(counter.toString(), selfMachineName, msgs[0]);
            logPrint("ClientTask doInbackgroud : the size of socketSV: "+ String.valueOf(socketsSV.length));


            //recieve information regarding all 5or4 proposed sequences
            for (int i = 0; i < socketsSV.length; i++) {
                Socket socket_each = socketsSV[i];
                if (socket_each == null) {
                    continue;
                }
                try {
                    Scanner scanner = new Scanner(socket_each.getInputStream());
                    while (!scanner.hasNext()){;}
                    String[] cntnt = scanner.nextLine().split(" ");

                    pairProposedSeqList.add(new Pair<String, String>(cntnt[1], REMOTE_PORTS[i]));

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            //find the highest sequence number
            Integer skmax = 0;
            String kProc = "";
            for (Pair<String, String> pair_each:pairProposedSeqList){
                Integer newSk = Integer.parseInt(pair_each.first);
                if (newSk > skmax){
                    skmax = newSk;
                    kProc = pair_each.second;
                }else if(newSk.equals(skmax) &&  pair_each.second.compareTo(kProc) < 0){
                    kProc = pair_each.second;
                }
            }

            // TODO: Mid is not totally exact the value with a lot of them, should be a array
            BMulticastAgreedSeq(counter.toString(), selfMachineName, skmax.toString(), kProc);

            // close all sockets
            for (Socket socket_each:socketsSV){
                try{
                    socket_each.close();
                }catch (IOException e){
                    Log.e(TAG, e.getMessage());
                }
            }

            return null;
        }

        private Socket[] BMulticastMSG(String cnteri, String proci, String content){
            Socket[] retSockets = new Socket[REMOTE_PORTS.length];
            String strToSend = cnteri + " " + proci + " " + content;

            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                try {
                    String portToSend = REMOTE_PORTS[i];

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(portToSend));

                    retSockets[i] = socket0;

//                socket0.setSoTimeout(500);
                    // send messages
                    OutputStream outputStream;
                    try {
                        outputStream = socket0.getOutputStream();
                        outputStream.write((strToSend).getBytes());

                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "SocketTimeoutException :: Client");
                    } catch (Exception e) {
                        Log.e(TAG, "outputstream write" + e.getMessage());
                    }


                } catch (SocketException e){
                    Log.e(TAG, e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException"+e.getMessage());
                }
            }
            return retSockets;
        }

        private void BMulticastAgreedSeq(String mid, String proci, String sk, String prock){

            String strToSend = mid + " " + proci + " " + sk + " " + prock;

            logPrint("the size of socketSV: "+ String.valueOf(socketsSV.length));
            for (Socket socket_each:socketsSV){
                OutputStream outputStream;
                try{
                    outputStream = socket_each.getOutputStream();
                    outputStream.write(strToSend.getBytes());
                }catch (IOException e){
                    Log.e(TAG, e.getMessage());
                }
            }
        }

    }

    /////////////////////////  Class for parameters delivery    ///////////////////////////////
/*    public class ParaClient{
        String msgUniqueID;  // like "5554_1"
        String msg;
        HashMap<String, Socket> groupSockets;
        HashMap<String, Boolean> machineStatus;

        public ParaClient(String msgUniqueID, String msg, HashMap<String, Socket> groupSockets, HashMap<String, Boolean> machineStatus){
            this.msgUniqueID = msgUniqueID;
            this.msg = msg;
            this.groupSockets = groupSockets;
            this.machineStatus = machineStatus;
        }
    }*/

    public class TOQueueItem{
        String mID;
        String jProcSentMsg;
        String sProposedSeq;
        String kProcProposing;
        Boolean status;
        String msg;

        public TOQueueItem(String mID, String jProcSentMsg, String sProposedSeq, String kProcProposing, Boolean status,String msg){
            this.mID = mID;
            this.jProcSentMsg = jProcSentMsg;
            this.sProposedSeq = sProposedSeq;
            this.kProcProposing = kProcProposing;
            this.status = status;
            this.msg = msg;
        }
    }
    public class ComparatorTOQueueItem implements Comparator<TOQueueItem>{
        // How to use:
        // Comparator<User> cmp = new ComparatorUser();
        //Collections.sort(userlist, cmp);
        @Override
        public int compare(TOQueueItem lhs, TOQueueItem rhs) {
            int flag = Integer.parseInt(lhs.sProposedSeq) - (Integer.parseInt(rhs.sProposedSeq));
            if (flag == 0){
                return lhs.kProcProposing.compareTo(rhs.kProcProposing);
            }
            return flag;
        }
    }

    public void logPrint(String information){
        Log.e(TAG, information);
    }


    ///end *******************************************************///

}
