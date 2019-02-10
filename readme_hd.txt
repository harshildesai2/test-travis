# for travvis build:
1. commit into the branch present in local directory (/Users/hdesai/Downloads/test-travis)
2. for commit
	- git add . (to add all files for commit)
	- git commit 
	- git push
	
3. once push is done, travis is configured to automatically execute the mvn build and upload the jar to s3 bucket: lll-responsys-consentmgt/code


# for terraform 
1.	execute below command for initializing the terraform, if not already done
	- terraform init  

2.	execute command for creating the stack:
	- terraform execute