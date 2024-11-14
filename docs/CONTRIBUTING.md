# Contribution Guide

Hello there! Welcome to the CS 240 Autograder Repo. If you're new to the 
Autograder Development team, we're glad to have you here. The CS 240 
Autograder relies on TAs like you to maintain and adapt the codebase 
for our professors and students.

This document will give you all the info you need to know to successfully 
contribute to the best Autograder on campus!

## Table of Contents
- Welcome (You already read that)
- [New Contributors [Start here if you're new]](#new-contributors)
- [Development Pipeline](#development-pipeline)
  - [1: Issues](#1-issues)
    - [When to Create Issue](#when-to-create-an-issue)
    - [Creating an Issue](#creating-an-issue)
    - [Working on an Issue](#working-on-an-issue)
  - [2: Branches](#2-branches)
    - [Creating a Branch](#creating-a-branch)
  - [3: Making Changes](#3-making-changes)
    - [Writing Good Commit Messages](#writing-good-commit-messages)
  - [4: Pull Requests](#4-pull-requests)
    - [Creating a Pull Request](#creating-a-pull-request)
    - [Scope of a Pull Request](#scope-of-a-pull-request)
    - [Reviewing a Pull Request](#reviewing-a-pull-request)
    - [After Merging](#after-merging)
- [About Page](#about-page)
- [Reminder About Ownership](#reminder-about-ownership)

## New Contributors
New to the Autograder Development team and not sure how to get 
started? Try this:

1. Finish reading this document
2. Get your dev environment set up by following the [Getting Started Guide](getting-started/getting-started.md)
3. Review the Sequence/Class Diagrams (_Coming soon_)
4. Look through some of the code, and try writing documentation 
   for undocumented code
5. Find a section of under-tested code and add some unit tests
6. Take a look through the GitHub repo's [Issues](https://github.com/softwareconstruction240/autograder/issues) page
   and find one you like, especially (but not limited to) ones labeled
   ["good first issue"](https://github.com/softwareconstruction240/autograder/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)

Don't be afraid to submit a PR, and most importantly, just get sucked in!

## Development Pipeline

As a broad overview, all changes to the Autograder start as an issue. Each 
issue eventually gets a branch made to solve the issue. And each branch
gets made into a pull request before becoming part of the main branch of code.

### 1. Issues

Issues help us track what needs to be done and who's working on what. You can pick an already 
existing issue to work on, or you can write your own. 

> [!TIP]
> Ideally, you should first work on issues in the `On Deck` column of the 
[TA Project Board](https://github.com/orgs/softwareconstruction240/projects/1/views/9),
but you are welcome to pick any unassigned issue

#### When to Create an Issue
TODO

#### Creating an Issue
- Create the issue from the TA Projects Board (linked above)
  - Click "add item" at the bottom of the `Todo` list
  - Type `#autograder` to connect the issue to the Autograder
  - Type a title and hit enter
- Use a clear, descriptive title that uses one of these prefixes (including the colon):
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
- Write tests for your code. All new features and functionality should have tests demonstrating correctness
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
- Push your code to GitHub at the end of each coding session. This lets other
  team members see how you're doing

#### Writing Good Commit Messages
- Start with an imperative statement that describes what the commit does:
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

For bigger changes, you can add more details after the first line (leave a blank line between the first
line and the extra details).

> [!TIP]
> Remember: A good commit message lets other developers know what changed
> without having to look at the code!

### 4. Pull Requests
Pull requests are how you get your changes merged into the codebase.
You create a pull request once you feel your changes are ready. You can also create a draft pull request 
if you would like some feedback on your code before then.

#### Creating a Pull Request
- Make sure you have pushed all changes to GitHub first
- The title of your pull request should be simple, concise, and descriptive
  - It should have a prefix just like an issue
  - It can be basically the name of the branch
    - Issue: `Frontend: refactor all server calls to one place`
    - Branch: `server-communicator`
    - Pull Request: `Frontend: ServerCommunicator`
- Fill out the Pull Request template with enough detail for others to understand what 
  changed without having to read all the new code.
- Make sure all GitHub actions pass:
  - Your pull request will be blocked from merging if all tests don't pass
- Move the connected issue into the `PR Submittted` column on the 
  [TA Project Board](https://github.com/orgs/softwareconstruction240/projects/1/views/9)
- Request a review from other Autograder developers
  - If you're changing an existing system, request a review from the developer that 
    wrote it initially, or is most familiar with that system
  - You can request multiple reviewers, but you're only required to get approval from one

#### Scope of a Pull Request
- Pull Requests should represent a complete, reviewable unit of work
- In most cases, a pull request from one issue
  - Multiple issues in one PR are fine and encouraged when they depend on each other, or form a cohesive change
- While there's no strict size limit, consider breaking up very large changes if:
  - They touch multiple unrelated areas
  - They're becoming difficult to review
  - They could logically be shipped separately

> [!TIP]
> Remember: A good PR represents a complete feature or fix, but shouldn't try to solve everything at once!

#### Reviewing a Pull Request
A crucial part of this process is the code review. The repo has been set up so that _**no changes may be merged 
into main without the review of at least one other developer.**_ If someone has requested a review from you, please
take some time to help out and ensure the Autograder has quality code.

**As a Reviewer**
- Look for:
  - Code quality and style guide compliance
  - Test coverage
  - Potential bugs or edge cases
  - Documentation completeness
- Checkout the branch on your local machine and do some manual testing
  - Specifically verify new/changed systems work. Try breaking it.
  - Generally check other systems still work and haven't been broken
    - (Student submissions on test student is most important)
- Be constructive and kind in your feedback. We're all learning here
- If changes are needed:
  - Be specific about what needs to change
  - Explain why the change is needed
  - Suggest how to make the change if possible
- Approve the PR when you're satisfied with all changes

**As a Pull Request Author**
- Respond to reviewer comments promptly
- Be open to feedback and suggestions
- Explain your decisions when asked
- Make requested changes or explain why they shouldn't be made
- Mark conversations as resolved once you have addressed the concern
- Re-request their review
- Thank reviewers for their time

> [!IMPORTANT]
> Once the pull request is approved and ready to merge, it is the responsibility and privilege of the author
> (not the reviewer) to merge the request.

> [!NOTE]
> Once a pull request is merged to main, it does not immediately deploy to the live Autograder. Changes will
> not be reflected in the live system until the head Autograder developer releases a new version of the system.

#### After Merging
Congrats! You just added code to the Autograder!

Make sure you move your issue on the [TA Project Board](https://github.com/orgs/softwareconstruction240/projects/1/views/9)
to the `Done` column

Celebrate!

## About Page
A lot of time and effort goes into developing and maintaining the Autograder. Thank you!

If you're new to the team, add yourself to the Autograder's [About Page](../src/main/resources/frontend/src/components/AboutPage.vue). (You can see the live one by doing the Konami Code
anywhere on the Autograder front end)

Fill this component out and add it to the bottom of the current list of developers:
```vue
<AboutPagePerson
  name=""
  url=""
  tenure=""
  contributions=""
  fa-icon=""/>
```
Most TAs just link to their GitHub profile on their URL.

Tenure should indicate when you worked as a CS 240 TA, not just as an Autograder Developer.

Write a short sentence describing your work on the Autograder. You can update this in the future as you make more contributions.

FA-icon is a font-awesome icon. Search through [Font Awesome's free icon collection](https://fontawesome.com/search?o=r&m=free) 
and choose one to represent you.

## Reminder About Ownership
As BYU employees writing code for our job as TAs, all code you contribute becomes the
intellectual property of Brigham Young University.

> Pursuant to law and university policy, any work (whether a Technical Work or a Creative Work) prepared by University 
> Personnel within the scope of their employment, without an express agreement specifying otherwise, is work for hire 
> owned by the university.
> 
> — [BYU Intellectual Property Policy](https://policy.byu.edu/view/intellectual-property-policy) (emphasis added)
