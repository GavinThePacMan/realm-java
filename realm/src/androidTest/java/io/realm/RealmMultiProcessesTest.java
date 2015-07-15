package io.realm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;

import io.realm.entities.AllTypes;
import io.realm.services.InterProcessesService;

public class RealmMultiProcessesTest extends AndroidTestCase {
    private Realm testRealm;
    private Messenger serviceMessenger;
    private Messenger receiverMessenger;
    private final CountDownLatch serviceLatch = new CountDownLatch(1);
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            serviceMessenger = new Messenger(iBinder);
            serviceLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @SuppressLint("HandlerLeak") // SuppressLint bug, doesn't work
    private class InterProcessHandler extends Handler {
        // Timeout Watchdog. In case the service crashed or expected response is not returned.
        // It is very important to feed the dog after the expected message arrived.
        private final int timeout = 2000;
        private boolean timeoutFlag = true;
        private Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeoutFlag) {
                    assertTrue("Timeout happened", false);
                } else {
                    timeoutFlag = true;
                    postDelayed(timeoutRunnable, timeout);
                }
            }
        };

        protected void clearTimeoutFlag() {
            timeoutFlag = false;
        }

        protected void done() {
            Looper.myLooper().quit();
        }

        public InterProcessHandler(Runnable startRunnable) {
            super(Looper.myLooper());
            receiverMessenger = new Messenger(this);
            post(startRunnable);
            postDelayed(timeoutRunnable, timeout);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String error = bundle.getString(InterProcessesService.BUNDLE_KEY_ERROR);
            if (error != null) {
                assertTrue(error, false);
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testRealm = Realm.getInstance(getContext());
        RealmConfiguration conf = testRealm.getConfiguration();
        testRealm.close();
        Realm.deleteRealm(conf);

        Intent intent = new Intent(getContext(), InterProcessesService.class);
        getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getContext().unbindService(serviceConnection);
    }

    private void sendToServiceProcess(int id) throws RemoteException {
        Message msg = Message.obtain(null, id);
        msg.replyTo = receiverMessenger;
        serviceMessenger.send(msg);
    }

    // 1. Main process create Realm, write one object.
    // A. Service process open Realm, check if there is one and only one object.
    public void testCreateInitialRealm() throws InterruptedException {
        serviceLatch.await();

        new InterProcessHandler(new Runnable() {
            @Override
            public void run() {
                try {
                    testRealm = Realm.getInstance(getContext());
                    assertEquals(testRealm.allObjects(AllTypes.class).size(), 0);
                    testRealm.beginTransaction();
                    testRealm.createObject(AllTypes.class);
                    testRealm.commitTransaction();

                    sendToServiceProcess(InterProcessesService.MSG_CreateInitialRealm_A);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }}) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == InterProcessesService.MSG_CreateInitialRealm_A) {
                    clearTimeoutFlag();
                    done();
                } else {
                    assertTrue(false);
                }
            }
        };
        Looper.loop();
    }
}
