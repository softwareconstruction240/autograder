# Special "Getting Started" instructions on Windows Machines

The autograder unfortunately won't work directly from Windows, so it must be run inside a Docker container
(recommended) or inside WSL (possible but can be finicky). When running the autograder inside a Docker container,
you will need to use a _Dev Container_ if you want to use IntelliJ's or VSCode's debugging tools.

Either way, running your MySQL server locally on your Windows machine should work for the database.

## Development inside a Dev Container (recommended)

The easiest way to debug a program running inside a Docker container is via a "Dev Container," which is essentially
just a fancy term for a Docker container that's linked to an IDE. It essentially allows you to run your IDE inside the
container, which enables debugging the autograder code on Windows machines. How it works is IDE-specific; this document
provides instructions for the autograder using IntelliJ. Further reading can be
found [here](https://www.jetbrains.com/help/idea/connect-to-devcontainer.html).

This requires having Docker installed and running on your machine. To develop with IntelliJ, it also requires the full
professional version (or a student license of it). The VSCode equivalent is free.

### IntelliJ

To do this in IntelliJ, navigate to `.devcontainer/devcontainer.json`. There should be an icon that pops up next to
the opening curly brace. Click the icon, then select `Create Dev Container and Clone Sources...`. (Not to be confused
with `Mount Sources`, which doesn't quite work. If the Clone Sources option does not appear, see below.) This should
pop up a dialog box that allows you to change a few options about the container. Look through them
and change what you need, then hit the `Build Container and Continue` button. Wait for IntelliJ and Docker to build
everything. You may need to click a few buttons along the way. Eventually a new IntelliJ window will pop up from the
dev container. Follow the [Getting Started](getting-started.md) steps with the new window.
Use `host.docker.internal` as your db-host argument; this tells docker to look for the database on your local Windows
machine rather than inside the container. To reopen the container after you've closed it, navigate to the
`.devcontainer/devcontainer.json` file again, click the icon, and select `Show Dev Containers`. Select the container
and it should reopen the second IntelliJ. If nothing appears, make sure the docker engine is running
(perhaps by opening Docker Desktop).

If you don't see an option to `Clone Sources`, you can do the same thing via JetBrains Gateway. Download and install
it if you haven't already. (Installing via JetBrains Toolbox is probably the easiest way.) Open Gateway, select
`Dev Containers`, and click `New Dev Container`. Choose `From VCS Project`, select Intellij IDEA from the dropdown,
and paste a link to the autograder GitHub repo (or your fork of it). The remainder of the setup after clicking
`Build Container and Continue` is the same as above, except that to reopen the container after you've closed it, you'll
need to open Gateway again and select the dev container from there.

### VSCode

Go fish🐟 These instructions are not included in this file. It should be possible to simply use VSCode's Dev Containers
extension, though.

## Development inside WSL

If Docker doesn't work for some reason, you can use WSL. You'll be able to simply clone and run the autograder, except
that everything in these instructions should be done via a WSL terminal _and_ inside a WSL directory. If you use a
Windows directory, like Desktop/chess, you might run into file permissions errors. "Unix-like" shells, such as Git
Bash, will not work: it needs to be a true WSL terminal.

For the `--db-host` program argument, you can't simply use `localhost` (assuming your database is running on Windows),
since that will refer to the WSL instance. Running `echo $(hostname)` from a WSL terminal will tell you what your
computer's true hostname is (ex. `LAPTOP-ABC123`). Appending `.local` to that (ex. `LAPTOP-ABC123.local`) gives you
the hostname that WSL uses to refer to the Windows machine. Use this as the `--db-host` program argument.

By default, MySQL users have "Limit to Host Matching" set to `localhost`, which does not allow requests coming from the
WSL virtual machine. You will have to expand this in MySQLWorkbench (under Server, click "Users and Privileges"). The
easiest way is to change it to `%`, which allows all hostnames (but it is highly recommended that you only do this for a
new user with restricted privileges, rather than using root). Another option is to run `wsl hostname -I` in PowerShell
to determine the WSL instance's IP address, then simply hardcode that, but this IP may change when WSL restarts.