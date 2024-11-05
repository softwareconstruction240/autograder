# Contribution Guide

Hello there! Welcome to the CS 240 Autograder Repo! If you're new to the 
Autograder Development team, we're glad to have you here! The CS 240 
Autograder relies on TAs like you to maintain and adapt the codebase 
for our professors and students.

This document will give you all the info you need to know to successfully 
contribute to the best Autograder on campus!

## New Contributors
New to the Autograder Development team and not sure how to get 
started? Try this:

1. Finish reading this document
2. Get set you dev environment up by following the [Getting Started Guide](docs/getting-started/getting-started.md)
3. Review the Sequence/Class Diagrams (_Coming soon_)
4. Look through some of the code, and try writing documentation 
   for undocumented code
5. Find a section of under-tested code and add some unit tests
6. Take a look through the GitHub repo's [Issues](https://github.com/softwareconstruction240/autograder/issues) page
   and find one you like, especially (but not limited to) ones labeled
   ["good first issue"](https://github.com/softwareconstruction240/autograder/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)

Don't be afraid to submit a PR, and most importantly, just get sucked in!

## Development Pipeline

### 1. Issues

All changes to the Autograder start as an issue. Issues help us track what 
needs to be done and who's working on what. You can pick an already 
existing issue to work on, or you can write your own. 

Ideally, you should first work on issues in the `On Deck` column of the 
[TA Projects Board](https://github.com/orgs/softwareconstruction240/projects/1/views/9),
but you are welcome to pick any unassigned issue

#### Creating an Issue
- Create the issue from the TA Projects Board (linked above)
  - Click "add item" at the bottom of the `Todo` list
  - Type `#autograder` to connect the issue to the Autograder
  - Type a title and hit enter
- Use a clear, descriptive title that uses one of these prefixes:
  - `Backend:` server side changes
  - `Frontend:` client (web) side changes
  - `Fullstack:` for issues that require front and backend changes
  - `Docs:` changes to documentation (either markdown files or JSDocs/JavaDocs)
  - `Tests:` adding or modifying tests
  - `Dev:` changes to enhance the Autograder development experience
- Provide enough context so others can understand the purpose
- Add labels as appropriate ([Here's the list](https://github.com/softwareconstruction240/autograder/issues/labels))
- Link related issues
- Issues should be narrow in scope

#### Working on an Issue
- Assign the issue to yourself before beginning work (this is how we know 
  who is working on what, and gives you a sort of ownership over the issue)
- Create a new branch ([more info here](#2-branches))
- If you find a new issue, file it separately (don't mix concerns)
- Leave a comment from time to time with updates on your progress

### 2. Branches
