.PHONY : deploy

deploy :
	git checkout -b heroku-deployment-branch
	yarn run shadow-cljs release server
	git add out --force
	git push heroku heroku-deployment-branch:master --force
	git checkout master
	git branch -d heroku-deployment-branch
	git reset HEAD out
