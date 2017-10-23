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
}
