package Logic.Communication;

import CRC8.CRC8;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.ArrayList;

/**
 * Wraps the communication to and from the ASR robot.
 */
public class ASRCommunication implements SerialPortDataListener {
    private SerialPort comPort;
    private ASRInitiater asrInitiater;

    public ASRCommunication(SerialPort port) {
        comPort = port;
        comPort.openPort();
        comPort.addDataListener(this);
        asrInitiater = new ASRInitiater();
        start();
    }

    public static void main(String[] args) {
        SerialPort port = SerialPort.getCommPorts()[0];
        port.setBaudRate(115200);
        ASRCommunication r = new ASRCommunication(port);

        r.start();
        r.gotoPos((byte) 3, (byte) 2);
    }

    /**
     * Subscribe to the events from the ASR.
     * @param listener
     */
    public void subscribeToResponses(ASRListener listener) {
        asrInitiater.addASRListener(listener);
    }

    /**
     * Send the given packet to the robot.
     * @param packet
     */
    public void sendPacket(Packet packet) {
        byte[] bytes = packet.getBytes();

        comPort.writeBytes(bytes, bytes.length);
    }

    /**
     * Move the robot to a certain x,y position.
     * @param x
     * @param y
     */
    public void gotoPos(int x, int y) {
        byte[] payload = { (byte) x, (byte) y };
        Packet packet = new Packet((byte) 11, payload);

        sendPacket(packet);
    }

    /**
     * Pick the order at a certain position
     */
    public void pick(){
        Packet p = new Packet((byte) 13, new byte[0]);
        sendPacket(p);
    }

    /**
     * Send the start command to the robot.
     */
    public void start(){
        Packet p = new Packet((byte) 3, new byte[0]);
        sendPacket(p);
    }

    /**
     * Send the stop command to the robot.
     */
    public void stop(){
        Packet p = new Packet((byte) 2, new byte[0]);
        sendPacket(p);
    }

    /**
     * Get the current position of the robot
     */
    public void getPos(){
        Packet p = new Packet((byte) 10, new byte[0]);
        sendPacket(p);
    }

    /**
     * Unload the picked products.
     */
    public void unload(){
        Packet p = new Packet((byte) 14, new byte[0]);
        sendPacket(p);
    }

    @Override
    public int getListeningEvents() {
        return 0;
    }

    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
            return;
        try {
            Thread.sleep(3000);
        } catch (Exception e) {

        }

        byte[] sizeBuffer = new byte[1];
        comPort.readBytes(sizeBuffer, 1);
        byte size = sizeBuffer[0];

        byte commandId = 0;
        while (commandId == 0) {
            byte[] commandBuf = new byte[1];
            comPort.readBytes(commandBuf, 1);
            commandId = commandBuf[0];
        }

        byte[] payload = new byte[size];

        int timeout = 100;

        while (comPort.bytesAvailable() < size + 1) {
            try {
                System.out.println("Not all received - " + comPort.bytesAvailable() + "/ " + (size + 1) + " bytes");
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("All " + (size + 1) + " bytes received");
        comPort.readBytes(payload, size);

        System.out.println("Payload content:");
        for (int i = 0; i < size; i++) {
            System.out.println(i + 1 + ": " + payload[i]);
        }

        byte checkBuffer[] = new byte[1];
        comPort.readBytes(checkBuffer, 1);
        long packetChecksum = checkBuffer[0];

        byte packet[] = new byte[size + 2];

        packet[0] = size;
        packet[1] = commandId;

        System.arraycopy(payload, 0, packet, 2, payload.length);

        CRC8 receivedChecksum = new CRC8();
        receivedChecksum.update(packet, 0, packet.length);
        long calcChecksum = receivedChecksum.getValue();

        System.out.println("Received Check: " + packetChecksum + ", Calculated Checksum: " + calcChecksum);

        if (packetChecksum == calcChecksum) {
            System.out.println("Packet is valid");

            if (commandId == 101) {
                System.out.println("Response to GetStatus (101)");
                if (size == 1) {
                    System.out.println("Status " + payload[0]);
                    System.out.println("Response is correct");
                    // TODO: Add application call
                } else {
                    System.err.println("Size differs from expected");
                }
            }

            if (commandId == 102) {
                System.out.println("Response to Stop (102)");
                if (size == 1) {
                    System.out.println("Response is correct");
                    // TODO: Add application call
                }
            }

            if (commandId == 103) {
                System.out.println("Response to Start (103)");
                if (size == 1) {
                    System.out.println("Response is correct");
                    // TODO: add application call
                } else {
                    System.err.println("Size differs from expected");
                }
            }

            if (commandId == 104) {
                System.out.println("Message response (104)");
                if (size == 1) {
                    System.out.println("Response is correct");

                    for (char c : payload) {
                        System.out.print(c);
                    }

                    System.out.print("\n");
                    // TODO: add application call
                } else {
                    System.err.println("Size differs from expected");
                }
            }

            // getPos response 110
            if (commandId == 110) {
                System.out.println("Response to getPos (110)");
                if (size == 2) {
                    System.out.println("Asr is at position x: " + payload[0] + ", y: " + payload[1]);

                    // TODO: add application call
                } else {
                    System.err.println("Size differs from expected");
                }
            }

            // gotoPos reponse 111
            if (commandId == 111) {
                if (size == 1) {
                    if (payload[0] == 0) {
                        System.out.println("GotoPos success");

                        // TODO: Add application call
                    } else {
                        System.out.println("GotoPos went wrong");
                    }
                } else {
                    System.err.println("size differs from expected");
                }
            }

            if (commandId == 113) {
                if (size == 1) {
                    if (payload[0] == 0) {
                        System.out.println("Pick succes");

                        // TODO: Add application call
                    } else {
                        System.out.println("Pick went wrong");
                    }
                } else {
                    System.err.println("size differs from expected");
                }
            }

            if (commandId == 114) {
                System.out.println("Response to unload (114)");
                if (size == 1) {
                    System.out.println("Response is correct");

                    // TODO: Add Application call
                } else {
                    System.err.println("size differs from expected");
                }
            }

        } else {
            System.err.println("Packet is invalid");
        }
    }
}