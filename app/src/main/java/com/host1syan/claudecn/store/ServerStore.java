package com.host1syan.claudecn.store;

import android.content.Context;
import android.content.SharedPreferences;
import com.host1syan.claudecn.model.ServerConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerStore {
    private static final String KEY_NAME = "name_";
    private static final String KEY_ORDER = "order";
    private static final String KEY_TOKEN = "token_";
    private static final String KEY_URL = "url_";
    private static final String PREF = "claudecn_servers";
    private final SharedPreferences sp;

    public ServerStore(Context context) {
        this.sp = context.getApplicationContext().getSharedPreferences(PREF, 0);
    }

    public List<ServerConfig> list() {
        ArrayList arrayList = new ArrayList();
        String string = this.sp.getString(KEY_ORDER, "");
        if (string != null && !string.isEmpty()) {
            for (String str : string.split(",")) {
                if (!str.isEmpty()) {
                    String string2 = this.sp.getString(KEY_URL + str, "");
                    if (string2 != null && !string2.isEmpty()) {
                        arrayList.add(new ServerConfig(str, this.sp.getString(KEY_NAME + str, ""), string2, this.sp.getString(KEY_TOKEN + str, "")));
                    }
                }
            }
        }
        return arrayList;
    }

    public ServerConfig get(String str) {
        for (ServerConfig serverConfig : list()) {
            if (serverConfig.id.equals(str)) {
                return serverConfig;
            }
        }
        return null;
    }

    public void save(ServerConfig serverConfig) {
        SharedPreferences.Editor edit = this.sp.edit();
        edit.putString(KEY_NAME + serverConfig.id, serverConfig.name == null ? "" : serverConfig.name);
        edit.putString(KEY_URL + serverConfig.id, serverConfig.url == null ? "" : serverConfig.url);
        edit.putString(KEY_TOKEN + serverConfig.id, serverConfig.token != null ? serverConfig.token : "");
        ArrayList arrayList = new ArrayList();
        Iterator<ServerConfig> it = list().iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().id);
        }
        if (!arrayList.contains(serverConfig.id)) {
            arrayList.add(serverConfig.id);
        }
        edit.putString(KEY_ORDER, join(arrayList));
        edit.apply();
    }

    public void remove(String str) {
        ArrayList arrayList = new ArrayList();
        for (ServerConfig serverConfig : list()) {
            if (!serverConfig.id.equals(str)) {
                arrayList.add(serverConfig.id);
            }
        }
        SharedPreferences.Editor edit = this.sp.edit();
        edit.remove(KEY_NAME + str);
        edit.remove(KEY_URL + str);
        edit.remove(KEY_TOKEN + str);
        edit.putString(KEY_ORDER, join(arrayList));
        edit.apply();
    }

    public String newId() {
        long currentTimeMillis = System.currentTimeMillis();
        String l = Long.toString(currentTimeMillis, 36);
        int i = 1;
        while (true) {
            if (!this.sp.contains(KEY_URL + l)) {
                return l;
            }
            l = Long.toString(currentTimeMillis, 36) + "-" + i;
            i++;
        }
    }

    private static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(str);
        }
        return sb.toString();
    }
}
