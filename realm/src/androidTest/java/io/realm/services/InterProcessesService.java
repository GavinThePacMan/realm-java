package io.realm.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import io.realm.Realm;
import io.realm.entities.AllTypes;

/**
 * Helper service for multi-processes support testing.
 */
public class InterProcessesService extends Service {
    public static final String BUNDLE_KEY_ERROR = "error";

    public static final int MSG_CreateInitialRealm_A = 30;

    private Realm testRealm;

    private final Messenger messenger = new Messenger(new IncomingHandler());
    private Messenger client;

    public InterProcessesService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return super.onUnbind(intent);
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            client = msg.replyTo;
            switch (msg.what) {
                case MSG_CreateInitialRealm_A:
                    doCreateInitialRealm_A();
                    break;
            }
            super.handleMessage(msg);
        }
    }


    private void response(int id, String error) {
        try {
            Message msg = Message.obtain(null, id);
            if (error != null) {
                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_KEY_ERROR, error);
                msg.setData(bundle);
            }
            client.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private String currentLine() {
        StackTraceElement element = new Throwable().getStackTrace()[1];
        return element.getClassName() + " line " + element.getLineNumber() + ": ";
    }

    private void doCreateInitialRealm_A() {
        testRealm = Realm.getInstance(this);
        int expected = 1;
        int got = testRealm.allObjects(AllTypes.class).size();
        if (expected == got) {
            response(MSG_CreateInitialRealm_A, null);
        } else {
            response(MSG_CreateInitialRealm_A,
                    currentLine() + "expected: " + expected + ", but got " + got);
        }
        testRealm.close();
    }
}
