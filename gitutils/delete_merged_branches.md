# devops-utils
DevOps Utilities scripts for various tasks

## Delete Merged Branches
This utility is used to delete feature branches (i.e. feat1--xxx, feat2--xxx) from a set of repositories where the feature branches were already merged into the *develop* branch.  It takes two auguments as follows:  
*   The first is the *date* (in **YYYY-MM-DD** format) that filters the candidate merged branches up to this date.  Any merged branches that are before the *date* are deleted.
*   *repo list* is the file path to a text file containing the list of repositories this utility will process on execution.

For example:
    *./delete_merged_branches.sh  2019-12-31 repos-to-cleanup.txt*
