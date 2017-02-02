/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package quorum.view;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

public class Configuration {
    
    protected int processId;
    
    protected int autoConnectLimit;
    protected Map<String, String> configs;
    protected HostsConfig hosts;
           
    protected static String configHome = "";
    protected static String hostsFileName = "";
    private int[] initialView;

    public Configuration(int procId){
        processId = procId;
        init();
    }
    
    public Configuration(int processId, String configHomeParam){
        this.processId = processId;
        configHome = configHomeParam;
        init();
    }

     public Configuration(int processId, String configHomeParam, String hostsFileNameParam){
        this.processId = processId;
        configHome = configHomeParam;
        hostsFileName = hostsFileNameParam;
        init();
    }

    public int[] getInitialView() {
        return initialView;
    }
     
     
     
    public String getViewStoreClass() {
        String s = (String) configs.get("view.storage.handler");
        if (s == null) {
            return "quorum.views.DefaultViewStorage";
        } else {
            return s;
        }

    }
    
    protected void init(){
        try{
            hosts = new HostsConfig(configHome, hostsFileName);
            
            loadConfig();
            
            String s = (String) configs.remove("system.autoconnect");
            if(s == null){
                autoConnectLimit = -1;
            }else{
                autoConnectLimit = Integer.parseInt(s);
            }
            
            
             s = (String) configs.remove("system.initial.view");
            if (s == null) {
                initialView = new int[3];
                for (int i = 0; i < 3; i++) {
                    initialView[i] = i;
                }
            } else {
                StringTokenizer str = new StringTokenizer(s, ",");
                initialView = new int[str.countTokens()];
                for (int i = 0; i < initialView.length; i++) {
                    initialView[i] = Integer.parseInt(str.nextToken());
                }
            }
            
            

        }catch(Exception e){
            System.err.println("Wrong system.config file format.");
            e.printStackTrace(System.out);
        }
    }
     
    public final boolean isHostSetted(int id){
        if(hosts.getHost(id) == null){
            return false;
        }
        return true;
    }
    
    
    
    public final int getAutoConnectLimit(){
        return this.autoConnectLimit;
    }
    
    public final String getProperty(String key){
        Object o = configs.get(key);
        if( o != null){
            return o.toString();
        }
        return null;
    }
    
    public final Map<String, String> getProperties(){
        return configs;
    }
    
    public final InetSocketAddress getRemoteAddress(int id){
        return hosts.getRemoteAddress(id);
    }
    

    public final InetSocketAddress getServerToServerRemoteAddress(int id){
        return hosts.getServerToServerRemoteAddress(id);
    }

    
    public final InetSocketAddress getLocalAddress(int id){
        return hosts.getLocalAddress(id);
    }
    
    public final String getHost(int id){
        return hosts.getHost(id);
    }
    
    public final int getPort(int id){
        return hosts.getPort(id);
    }
    
    
    public final int getProcessId(){
        return processId;
    }

    public final void addHostInfo(int id, String host, int port){
        this.hosts.add(id,host,port);
    }
    
    private void loadConfig(){
        configs = new Hashtable<String, String>();
        try{
            if(configHome == null || configHome.equals("")){
                configHome="config";
            }
            String sep = System.getProperty("file.separator");
            String path =  configHome+sep+"system.config";;
            FileReader fr = new FileReader(path);
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            while((line = rd.readLine()) != null){
                if(!line.startsWith("#")){
                    StringTokenizer str = new StringTokenizer(line,"=");
                    if(str.countTokens() > 1){
                        configs.put(str.nextToken().trim(),str.nextToken().trim());
                    }
                }
            }
            fr.close();
            rd.close();
        }catch(Exception e){
            e.printStackTrace(System.out);
        }
    }
}
