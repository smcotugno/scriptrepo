#!/bin/bash
set -e

if [ "$1" == "-help" ]; then
  echo "usage: ./delete_merged_branches.sh [ARGUMENT 1] [ARGUMENT 2]"
  echo "       where [ARUGMENT 1] = Date in YYYY-MM-DD format.  All merged brances before this date will be deleted"
  echo "       and   [ARGUMENT 2] = Path to a text file containing the list of repos to clean up."
  exit 0
fi

#  TO DO - Need to check format for date YYYY-MM-DD format
if [ -z "$1" ]
  then
    echo "arg1 Missing -  Date in YYYYMMDD format.  All merged brances before this date will be deleted"
    exit 1
fi
if [ -z "$2" ]
  then
    echo "arg2 Missing -  Path to the file containing the list of repos to clean up."
    exit 1
fi

DATE_DELETE_UPTO=$(echo $1 | tr -d "-")
REPO_LIST_FILE=$2

GITHUB_USERID="TBD"
MAIN_BRANCH="develop"

IFS=$'\r\n' GLOBIGNORE='*' command eval  "repo_array=($(cat ${REPO_LIST_FILE}))"

for index in ${!repo_array[@]}; do

    #skip comments
    [[ "${repo_array[index]}" =~ ^#.*$ ]] && continue

    #clone repos repository
    git clone git@github.${GITHUB_USERID}/${repo_array[index]}.git
    cd ${repo_array[index]}
    git switch ${MAIN_BRANCH}

    echo "Main Branch is ${MAIN_BRANCH}"

    git branch -v -r --merged ${MAIN_BRANCH} --sort=committerdate --format=' %(committerdate:short)%09%(refname:short)' | while read -r dateStr branchStr  ; 
    do
      origin=$(echo $branchStr | cut -f1 -d/)
      branch=$(echo $branchStr | cut -f2 -d/)
      if [ ${branch} != ${MAIN_BRANCH} ]    # DO NOT DELETE THE MAIN BRANCH
      then
        if [ ${branch} != "HEAD" ]          # DO NOT DELETE THE HEAD
        then
           # Delete branchs up to the given date
           dateNum=$(echo $dateStr | tr -d "-")
           if [ $dateNum -lt ${DATE_DELETE_UPTO} ] 
           then
             echo  "NEED TO DELETE: $dateStr   $origin   $branch"
             # TO  DO
             # git push --delete $origin $branch    #  delete remote branch
           else
             echo "NEWER NOT DELETED: $dateStr   $origin   $branch"
           fi
        fi
      fi
    done
      
    #remove repo folder
    cd ..
    rm -rf ${repo_array[index]}
    
    echo "====================================================================="

done
