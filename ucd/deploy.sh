#!/bin/bash
############################################
# Author: Steve Cotugno
# Date:   6/15/2017
# Purpose: This script will call the UCD REST interface to request an Application process to run an environment
# References
#   https://www.ibm.com/support/knowledgecenter/en/SS4GSP_6.2.4/com.ibm.udeploy.api.doc/topics/rest_cli_applicationprocessrequest_request_put.html
#
# example Execution
# ./deploy admin admin WFD Deploy_WFD DEV wfd-entree build-wfd-entree-50
#
#############################################

# Input paramaters
userName=$1
password=$2
appName=$3
appProcess=$4
envName=$5
compName=$6
compVersion=$7

# other parameters that are set here for now
ucdURL=https://ss-ucd.rtp.raleigh.ibm.com:8443
onlyChange=false


generate_post_data()
{
  cat <<EOF
{
    "application": "$appName",
    "description": "Executed from curl script that executed the UCD REST interface",
    "applicationProcess": "$appProcess",
    "environment": "$envName",
    "onlyChanged": "$onlyChange",
    "versions":  [{
       "version":   "$compVersion",
       "component": "$compName" 
     }]
}
EOF
}


echo "JSON is: $(generate_post_data)"

curl -S -i -k -u ${userName}:${password} \
-H "Accept: application/json" \
-H "Content-Type:application/json" \
-X PUT -d "$(generate_post_data)" "${ucdURL}/cli/applicationProcessRequest/request"

echo "The RESPONSE text is $_"
