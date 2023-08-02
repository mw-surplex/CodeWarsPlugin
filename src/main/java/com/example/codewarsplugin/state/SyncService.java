package com.example.codewarsplugin.state;

import com.example.codewarsplugin.services.login.LoginService;
import com.example.codewarsplugin.services.UserService;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SyncService {
    private static List<StateParams> stateParamsList = new ArrayList();

    public static List<StateParams> getStateParamsList() {
        return stateParamsList;
    }

    public static void addParams(StateParams stateParams) {
        stateParamsList.add(stateParams);
    }

    public static void remove(StateParams stateParams) {
        stateParamsList.remove(stateParams);
    }

    public static void login(){
        for(StateParams p : stateParamsList) {
            var store = p.getStore();
            store.getCurrentView().cleanup();
            store.getPreviousViews().add(store.getCurrentView());
            store.setCurrentView(store.getLogedInView());
            store.getCurrentView().setup();
        }


    }

    public static void logout() {
        SwingWorker<Void, Void> worker = new SwingWorker<>(){
            @Override
            protected Void doInBackground() {
                LoginService.logout();
                UserService.clearUser();
                return null;
            }
            @Override
            protected void done(){
                for(StateParams p : stateParamsList) {
                    var store = p.getStore();
                    store.getCurrentView().cleanup();
                    store.getPreviousViews().add(store.getCurrentView());
                    store.setCurrentView(store.getLoginView());
                    store.getCurrentView().setup();
                }
            }
        };
        worker.execute();
    }

}
