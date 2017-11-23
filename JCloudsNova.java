import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.v2_0.domain.Resource;

import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions.Builder;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import sun.net.www.http.HttpClient;

import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.neutron.v2.domain.Network.CreateNetwork;
import org.jclouds.openstack.neutron.v2.domain.Network.CreateBuilder;
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
	      case "listContainers":
		System.out.println("Listin containers...");
		jcloudsNova.listContainers();
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
      System.out.println("!");

      con.setRequestProperty("Accept-Encoding", "gzip");
      con.setRequestProperty("User-Agent", "UFRJ-Client (v0.1)");

/*
      con.setDoOutput(true);
      DataOutputStream out = new DataOutputStream(con.getOutputStream());
      out.writeBytes(JCloudsNova.getParamsString(parameters));
      out.flush();
      out.close();
*/     
//      String msg = con.getResponseMessage();

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

    //TODO: Não funciona ainda!!!!!!!!!!!!!!!!!
    private void createContainer(String name) throws IOException {
	HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
	JSONArray a = (JSONArray) parser.parse(new FileReader("c:\\exer4-courses.json"));

	String json = objectMapper.writeValueAsString(someObject);
	try {
    		HttpPost request = new HttpPost("http://localhost:6666/v1.32");
    		StringEntity params =new StringEntity("details={\"name\":\"myname\",\"age\":\"20\"} ");
    		request.addHeader("content-type", "application/x-www-form-urlencoded");
    		request.setEntity(params);
    		HttpResponse response = httpClient.execute(request);
	}catch (Exception ex) { 
	}

    //handle exception here

}
  
    }


    public void createServer(String name, String imageRef, String flavorRef, String network) {
      ServerApi serverApi = novaApi.getServerApi(region);
      CreateServerOptions options = CreateServerOptions.Builder.networks(network);
      serverApi.create(name, imageRef, flavorRef, options);
    }

    public void createNetwork(String name) {
      NetworkApi networkApi = neutronApi.getNetworkApi(region);
      networkApi.create(Network.CreateNetwork.createBuilder(name).build());
    }

    public void createSubnet(String network, String cidr) {
      SubnetApi subnetApi = neutronApi.getSubnetApi(region);
      subnetApi.create(Subnet.CreateSubnet.createBuilder(network, cidr).ipVersion(4).build());
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
}
