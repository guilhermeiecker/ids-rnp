import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.http.HttpResponse;
import org.jclouds.openstack.v2_0.domain.Resource;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.neutron.v2.domain.Subnet;
import org.jclouds.openstack.neutron.v2.features.SubnetApi;

public class JCloudsNova implements Closeable {
    private final NovaApi novaApi;
    private final NeutronApi neutronApi;
    private final String serverProvider, networkProvider, identity, credential, region;

    public static void main(String[] args) throws IOException {
        JCloudsNova jcloudsNova = new JCloudsNova();

        try {
            switch (args[0]) {
                // Openstack Features
                case "listServers":
                    System.out.println("Listing servers...");
                    jcloudsNova.listServers();
                    break;
                case "listImages":
                    System.out.println("Listing images...");
                    jcloudsNova.listImages();
                    break;
                case "listFlavors":
                    System.out.println("Listing flavors...");
                    jcloudsNova.listFlavors();
                    break;
                case "listNetworks":
                    System.out.println("Listing networks...");
                    jcloudsNova.listNetworks();
                    break;
                case "listSubnets":
                    System.out.println("Listing subnets...");
                    jcloudsNova.listSubnets();
                    break;
                case "createServer":
                    jcloudsNova.createServer(args[1], args[2], args[3], args[4]);
                    break;
                case "createNetwork":
                    jcloudsNova.createNetwork(args[1]);
                    break;
                case "createSubnet":
                    jcloudsNova.createSubnet(args[1], args[2]);
                    break;
                // Docker Features
                case "listContainers":
                    System.out.println("Listing containers...");
                    jcloudsNova.listContainers();
                    break;
                case "createContainer":
                    System.out.println("Creating container...");
                    jcloudsNova.createContainer(args[1]);
                    break;
                case "startContainer":
                    System.out.println("Starting container...");
                    jcloudsNova.startContainer(args[1]);
                    break;
                case "stopContainer":
                    System.out.println("Stopping container...");
                    jcloudsNova.stopContainer(args[1]);
                    break;
                case "removeContainer":
                    System.out.println("Removing container...");
                    jcloudsNova.removeContainer(args[1]);
                    break;
                default: System.out.println("Default");
            }
            jcloudsNova.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jcloudsNova.close();
        }
    }

    public JCloudsNova() {
        serverProvider = "openstack-nova";
        networkProvider = "openstack-neutron";
        identity = "admin:admin"; // tenantName:userName
        credential = "sdn1234";
        region = "RegionOne";

        novaApi = ContextBuilder.newBuilder(serverProvider)
                .endpoint("http://192.168.0.197/identity/v2.0") // servidor openstack na rnp ids2
                .credentials(identity, credential)
                .buildApi(NovaApi.class);

        neutronApi = ContextBuilder.newBuilder(networkProvider)
                .endpoint("http://192.168.0.197/identity/v2.0") // servidor openstack na rnp ids2
                .credentials(identity, credential)
                .buildApi(NeutronApi.class);
    }

    private void listServers() {
      ServerApi serverApi = novaApi.getServerApi(region);
      for (Resource server : serverApi.list().concat()) {
        System.out.println(server);
      }
    }

    private void listImages() {
      ImageApi imageApi = novaApi.getImageApi(region);
      for (Resource image : imageApi.list().concat()) {
        System.out.println(image);
      }
    }

    private void listFlavors() {
      FlavorApi flavorApi = novaApi.getFlavorApi(region);
      for (Resource flavor : flavorApi.list().concat()) {
        System.out.println(flavor);
      }
    }

    private void listNetworks() {
      NetworkApi networkApi = neutronApi.getNetworkApi(region);
      for (Network network : networkApi.list().concat()) {
        System.out.println(network);
      }
    }

    private void listSubnets() {
      SubnetApi subnetApi = neutronApi.getSubnetApi(region);
      for (Subnet subnet : subnetApi.list().concat()) {
        System.out.println(subnet);
      }
    }

    private void listContainers() throws IOException {
      String hostpor = "http://localhost:6666/";
      String version = "v1.32/";
      String command = "containers/json?";

      Map<String, String> parameters = new HashMap<>();
      parameters.put("all", "1");

      String urlString = hostpor + version + command + JCloudsNova.getParamsString(parameters);

      URL url = new URL(urlString); // Docker Daemon @ sdnoverlay.wb.land.ufrj
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");

      con.setRequestProperty("Accept-Encoding", "gzip");
      con.setRequestProperty("User-Agent", "UFRJ-Client (v0.1)");

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer content = new StringBuffer();
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
      }
      in.close();
      System.out.println(content);

      con.disconnect();
    }

    private void createContainer(String name) throws IOException {
        String hostpor = "http://localhost:6666/";
        String version = "v1.32/";
        String command = "containers/create?";

        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", name);

        String json = readFile("example2.json", StandardCharsets.UTF_8); //TODO: Checar se esse método funciona para fazer parser do arquivo

        String urlString = hostpor + version + command + JCloudsNova.getParamsString(parameters);

        URL url = new URL(urlString); // Docker Daemon @ sdnoverlay.wb.land.ufrj
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("User-Agent", "UFRJ-Client (v0.1)");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        // Send JSON
        OutputStream os = con.getOutputStream();
        os.write(json.getBytes("UTF-8"));
        os.close();

        // Read repsonse
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        con.disconnect();
    }

    public void createServer(String name, String imageRef, String flavorRef, String network) {
      ServerApi serverApi = novaApi.getServerApi(region);
      CreateServerOptions options = CreateServerOptions.Builder.networks(network);
      serverApi.create(name, imageRef, flavorRef, options);
    }

    /*public void createNetwork(String name) {
      NetworkApi networkApi = neutronApi.getNetworkApi(region);
      networkApi.create(Network.CreateNetwork.createBuilder(name).build());
    }*/

    public void createNetwork(String name, int tag, String physical){
    	NetworkApi netApi = neutronApi.getNetworkApi("RegionOne");
        Network network = netApi.create(Network.createBuilder(name).
				external(true).networkType(NetworkType.VLAN).
				segmentationId(tag).physicalNetworkName(physical).build());
         //assertNotNull(network);
    }

    public void createSubnet(String network, String cidr) {
      SubnetApi subnetApi = neutronApi.getSubnetApi(region);
      subnetApi.create(Subnet.CreateSubnet.createBuilder(network, cidr).ipVersion(4).build());
    }

    private void startContainer(String name) throws IOException {
        String hostpor = "http://localhost:6666/";
        String version = "v1.32/";
        String command = "containers/";

        String urlString = hostpor + version + command + name + "/start";

        URL url = new URL(urlString); // Docker Daemon @ sdnoverlay.wb.land.ufrj
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        // Read repsonse
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        con.disconnect();
    }

    private void stopContainer(String name) throws IOException {
        String hostpor = "http://localhost:6666/";
        String version = "v1.32/";
        String command = "containers/";

        String urlString = hostpor + version + command + name + "/stop";

        URL url = new URL(urlString); // Docker Daemon @ sdnoverlay.wb.land.ufrj
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        // Read repsonse
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        con.disconnect();
    }

    private void removeContainer(String name) throws IOException {
        String hostpor = "http://localhost:6666/";
        String version = "v1.32/";
        String command = "containers/";

        String urlString = hostpor + version + command + name;

        URL url = new URL(urlString); // Docker Daemon @ sdnoverlay.wb.land.ufrj
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("DELETE");

        // Read repsonse
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println(content);

        con.disconnect();
    }

    public void close() throws IOException {
        Closeables.close(novaApi, true);
        Closeables.close(neutronApi, true);
    }

    private static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
          result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
          result.append("=");
          result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
          result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
          ? resultString.substring(0, resultString.length() - 1)
          : resultString;
    }

    private static String readFile(String path, Charset encoding) throws IOException {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
    }
}
