.PHONY : deploy

deploy :
	git checkout -b heroku-deployment-branch
	yarn run shadow-cljs release server
	git add --force out/server/
	git commit -m "Deploy"
	git push heroku heroku-deployment-branch:master --force
	git checkout master
	git branch -D heroku-deployment-branch
	git reset HEAD out
