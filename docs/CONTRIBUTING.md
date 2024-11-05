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
2. Get set you dev environment up by following the [Getting Started Guide](getting-started/getting-started.md)
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

> Ideally, you should first work on issues in the `On Deck` column of the 
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
Branches in git allow us to all work on separate things in the same codebase.
You will do all your work in a branch.

#### Creating a branch
Create the branch directly from the issue so your work is linked.

- Always branch from `main` 
  - (exceptions exist if you know what you're doing)
- Use descriptive names that reflect what you're working on:
    - `add-late-submission`
    - `admin-display-verification-status`
    - `server-communicator`
    - `extract-service-logic-from-controllers`
- Use kebab-case (lowercase with hyphens)
- Keep names concise but clear
- One branch per issue (don't mix different features/fixes)

### 3. Making Changes
Now that you have a branch, you can start to code! Hurray! 🎉
Here are important guidelines to follow:

- Review and follow the Style Guide (_Coming Soon_)
- Write tests for your code. All new features and functionality must have tests demonstrating correctness
  - Update existing tests if you change functionality
  - _New contributors: A great way to better understand the codebase is to try writing tests for existing code_
- Keep commits focused and meaningful
  - Each commit should represent one logical change
  - Write clear commit messages that describe that change ([See guidelines](#writing-good-commit-messages))
- Update documentation as you go
  - Add JSDoc/JavaDoc comments to new code
    - _New contributors: Another great way to learn the codebase is to document existing code_
  - Update repo Markdown docs if needed
  - Code should for the most part be self-documenting. If not, make sure to add inline comments

#### Writing Good Commit Messages
- Start with a present-tense verb that describes what the commit does:
    - `add late submission validation`
    - `fix grade calculation bug`
    - `update setup instructions`
    - `remove unused imports`
    - `refactor test runner`
    - `document API endpoints`
- Use lowercase letters
- Keep it concise but clear (aim for under 50 characters)
- Bad examples:
    - `changes` (too vague)
    - `Added new feature` (past tense)
    - `WIP` (uninformative)
    - `Fix stuff` (too vague)

For bigger changes, you can add more details after the first line.

> Remember: A good commit message lets other developers know what changed
without having to look at the code!
