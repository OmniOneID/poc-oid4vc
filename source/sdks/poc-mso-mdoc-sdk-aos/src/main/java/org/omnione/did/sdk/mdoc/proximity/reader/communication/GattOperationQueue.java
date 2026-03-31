package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Android BLE는 한 번에 하나의 GATT 작업만 허용.
 * 이 큐를 통해 descriptor 쓰기, characteristic 쓰기 등을 순차 실행한다.
 */
public class GattOperationQueue {
    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private boolean operationPending = false;

    public synchronized void enqueue(Runnable op) {
        queue.add(op);
        processNext();
    }

    public synchronized void onOperationCompleted() {
        operationPending = false;
        processNext();
    }

    public synchronized void clear() {
        queue.clear();
        operationPending = false;
    }

    private void processNext() {
        if (operationPending) return;
        Runnable next = queue.poll();
        if (next != null) {
            operationPending = true;
            next.run();
        }
    }
}
