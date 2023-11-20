import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

public class App {
    public static void main(String[] args) throws Exception {

        // Configuration
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("emresnmp"));
        target.setAddress(GenericAddress.parse("127.0.0.1/161"));
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        
        // OIDs
        //doWalkIfTable(".1.3.6.1.2.1.2.2", target); // ifTable, mib-2 interfaces
        doGetRequest(".1.3.6.1.2.1.1.1.0", target); // sysDescr
        
    }


    public static void doGetRequest(String oid, Target target) throws IOException {
        long start = System.currentTimeMillis();
        TransportMapping<? extends Address> transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        ResponseEvent responseEvent = snmp.send(pdu, target);
        if (responseEvent != null && responseEvent.getResponse() != null) {
            VariableBinding vb = responseEvent.getResponse().get(0);
            System.out.println("Response for " + oid + ": " + vb.getVariable());
        } else {
            System.out.println("Failed to get response or response is null.");
        }
        snmp.close();
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        System.out.println("Elapsed time: " + elapsed + "ms");
    }

    // Walk the mib2.interfaces.ifTable
    public static void  doWalkIfTable(String tableOid, Target target) throws IOException {
        Map<String, String> result = new TreeMap<>();
        
        // Receiving snmp messages
        TransportMapping<? extends Address> transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();
 
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        List<TreeEvent> events = treeUtils.getSubtree(target, new OID(tableOid));
        if (events == null || events.size() == 0) {
            System.out.println("Error: Unable to read table...");
            return;
        }
 
        for (TreeEvent event : events) {
            if (event == null) {
                continue;
            }
            if (event.isError()) {
                System.out.println("Error: table OID [" + tableOid + "] " + event.getErrorMessage());
                continue;
            }
 
            VariableBinding[] varBindings = event.getVariableBindings();
            if (varBindings == null || varBindings.length == 0) {
                continue;
            }
            for (VariableBinding varBinding : varBindings) {
                if (varBinding == null) {
                    continue;
                }
                 
                result.put("." + varBinding.getOid().toString(), varBinding.getVariable().toString());
            }
 
        }
        snmp.close();
        

        // Print the results
        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (entry.getKey().startsWith(".1.3.6.1.2.1.2.2.1.2.")) {
                System.out.println("ifDescr" + entry.getKey().replace(".1.3.6.1.2.1.2.2.1.2", "") + ": " + entry.getValue());
            }
            if (entry.getKey().startsWith(".1.3.6.1.2.1.2.2.1.3.")) {
                System.out.println("ifType" + entry.getKey().replace(".1.3.6.1.2.1.2.2.1.3", "") + ": " + entry.getValue());
            }
        }
       
    }
}
