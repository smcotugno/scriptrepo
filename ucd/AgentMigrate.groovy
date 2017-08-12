//############################################
//# Date:   8/10/2017
//# Purpose: This groovy script  will swtich an agent from one agent relay to another.
//#          It assumes that the agent relay destination is in the same UCD instance
//#          
//# Setup:   In UCD, create a generic process and add the groovy script step.  Copy and paste this script into the step
//#          Create the following process properties: 
//#             targetJmsHost, targetJmsPort, httpProxyHost, httpProxyPort, server.url
//#          
//# References
//#   https://www.ibm.com/support/knowledgecenter/en/SS4GSP_6.2.4/com.ibm.udeploy.install.doc/topics/agent_properties.html
//#
//#############################################

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


public class AgentMigrate{
    private final static String fileSep      =System.getProperty("file.separator")

//# Get UCD Server values so that we can make a REST call to restart the agent after the updating the config 
    private final static String serverUrl    ="${p:server.url}"       
    private final static String authToken    =System.getenv()['AUTH_TOKEN']
    private final static String agentHome    =System.getenv()['AGENT_HOME']

//#   UCD property input values
    private final static String jmsHost      ="${p:targetJmsHost}"
    private final static String jmsPort      ="${p:targetJmsPort}"
    private final static String proxyHost    ="${p:httpProxyHost}"
    private final static String proxyPort    ="${p:httpProxyPort}"

    //# These properties are requied if changing the agent id.
    //# private final static String updateReg    ="${p:updateAgentRegistration}"
    //# private final static String agentReg     ="${p:agentRegistration}"
    //# private final static String agentId      ="${p:agent.id}"

	
    static void printMemberVars(){
       println("Target JMS HOST: ${jmsHost}")
       println("Target JMS PORT: ${jmsPort}")
       println("authToken      : ${authToken}")
       println("agent home     : ${agentHome}")
       println("agent config   : ${agentHome}${fileSep}conf${fileSep}agent${fileSep}installed.properties")
    }
    
    static void backupAgentPropertiesFile(){
        String installedPropFileName="${agentHome}${fileSep}conf${fileSep}agent${fileSep}installed.properties"
        try{
            AntBuilder ant=new AntBuilder()
            ant.copy(file: "${installedPropFileName}", tofile: "${installedPropFileName}.original")
        }
        catch(Exception ex){
            println("could not backup prop file")
            System.exit(1)
        }
    }

    static Properties loadAgentProperties(){
        String installedPropFileName="${agentHome}${fileSep}conf${fileSep}agent${fileSep}installed.properties"
        Properties agentProperties=new Properties()
        try{
            File propFile = new File(installedPropFileName)
            propFile.withInputStream{instream->
                agentProperties.load(instream)
            }
        }
        catch(Exception ex){
            println("could not load agent properties, abort script!")
            System.exit(1)
        }
        agentProperties.each(){key,value->
            println("${key}=${value}")
        }
        return agentProperties
    }

    private static void changeAgentProperties(Properties agentProperties){
        String brokerUrl="failover:(ah3://${jmsHost}:${jmsPort})"
        agentProperties.setProperty("locked/agent.jms.remote.port",jmsPort)
        agentProperties.setProperty("locked/agent.jms.remote.host",jmsHost)
        agentProperties.setProperty("locked/agent.brokerUrl",brokerUrl)
        agentProperties.setProperty("locked/agent.http.proxy.host",proxyHost)
        agentProperties.setProperty("locked/agent.http.proxy.port",proxyPort)
		
        //# This will update the agent ID if that option is selected
        //#	if(updateReg=="true"){
        //# 	    agentProperties.setProperty("locked/agent.id","${agentReg}")
	//#    }
    }
    
    private static void saveAgentProperties(Properties agentProperties){
        String installedPropFileName="${agentHome}${fileSep}conf${fileSep}agent${fileSep}installed.properties"
        File propsFile=new File(installedPropFileName)
        propsFile.withWriter('UTF-8'){writer->
            writer.writeLine ''
            agentProperties.each(){key,value->
                writer.writeLine "${key}=${value}"
            }
        }
    }
	
	private static void restartAgent(){
	    String restCall="${serverUrl}/rest/agent/${agentId}/restart"		
    	def nullTrustManager = [
            checkClientTrusted: { chain, authType ->  },
            checkServerTrusted: { chain, authType ->  },
            getAcceptedIssuers: { null }
        ]
        def nullHostnameVerifier = [
            verify: { hostname, session -> true }
        ]
        SSLContext sc = SSLContext.getInstance("SSL")
        sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
        URL url=new URL(restCall)
        String authString=("PasswordIsAuthToken:{\"token\":\"${authToken}\"}" as byte[]).encodeBase64().toString()
        HttpsURLConnection urlConnection=url.openConnection()
        urlConnection.setRequestProperty("Authorization", "Basic ${authString}")
        BufferedReader reader=new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))
        StringBuffer response=new StringBuffer()
        String inputLine
        while ((inputLine=reader.readLine()) != null){
                response.append(inputLine)
        }
        reader.close();
        println response.toString();
	}

    public static void main(String[] args){
        println "Test script"
        printMemberVars()
        backupAgentPropertiesFile()
        Properties agentProperties=loadAgentProperties()
        changeAgentProperties(agentProperties)
        saveAgentProperties(agentProperties)
        restartAgent()  //# Note that when it restarts it will be pointing to the new server or agent relay
    }

}

