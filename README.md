AgileReview-Findbugs
====================

Findbugs Plugin for AgileReview (Demo/Prototype)


How to use?
===========
* Install Findbugs Eclipse plugin e.g. via update site: http://findbugs.cs.umd.edu/eclipse/
* Import project in Eclipse, run as Eclipse Application (sry, no update site atm)
* Execute Findbugs
  * Right-click on project
  * Choose "Find Bugs-> Find Bugs"
* Import result in AgileReview
  * Right-click on project
  * Choose "AgileReview -> Import FindBugs result"

Current Status
==============
* What?
  * Running import in general works (fetching bugs, affected files, lines)
  * Creating a new review per import works
  * Transforming FindBugs findings into AgileReview comments works
* What not?
  * Need to switch to AgileReview perspective once before starting import
* Untested
  * Multi-line findings/comments
