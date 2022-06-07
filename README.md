
Watch the install guide video at https://rumble.com/v17oe2h-node-monitor-install-guide.html

Watch the CLI setup video at 

--------

Node Monitor is handy tool that shows you the current status of your Qortal node, your minting account and some other info about the network and your system. Its purpose is to show you how your Qortal node is performing in real-time.

You can choose to enable in-app and/or e-mail notifications when certain events are triggered:

- When your node goes offline, and when it comes back online again.
- When your node goes out of sync, and when it syncs up again (you decide the syncing threshold value).
- When your active minting account stops minting, and when it starts minting again (you decide after how many minutes).
- When your node's connected peers go below a specific number, and when it gains them again (you decide how many peers).

In-app notifications will pop up on the bottom right of your screen when they are triggered and you can read them in the notifications panel in the app. In order to receive e-mail notifications you'll have to setup your mail provider's SMTP settings. I've made the process as simple as possible, you can learn more by watching the install guide video.

For those of you running the Qortal core on a headless Pi, I've added a CLI (Command Line Interface) version which is basically just the e-mail notifier. You can setup the app through an SSH tunnel by running the 'launch-terminal.sh' script, when you are done with the setup you'll run the daemon (background) process by choosing 'Start' in the terminal menu. The daemon will send e-mails when your settings get triggered. The CLI version can only send e-mail notifications. You can download the CLI scripts on the GitHub releases page, watch the CLI setup video to learn more. 

Other features for the non-CLI version include:

- Customizable user interface styles, create your own or load preconfigured ones.
- Node sync status, including a time estimation for when your node will be in sync (based on your current syncing speed).
- Info about your active minting account, such as blocks minted, balance, level and level-up estimation based on your current minting efficiency.
- Price lookup for all QORT trading pairs and US Dollar.

------

You can run the program as follows:

For Windows:

double click the 'setup-windows.bat' file in your newly extracted folder, this will create a launcher file called 'Node Monitor'. Double click this file to run the program. If you ever decide to move the node-monitor folder to a different location you'll need to run the 'setup-windows.bat' file again, a new launcher will be created using the path variable of the new location.

For Mac and Linux:

right click the 'launch.sh' file and give it permission to run as an executable. Now open a terminal (console window) and drag and drop the 'launch.sh' file into the terminal, then press enter. 

Another option is to type the following in the terminal:

sudo chmod +x launch.sh

then press enter and type:

./launch.sh

 (don't forget the period) then enter. Make sure that you open the terminal window in the node-monitor folder.
 
 
To run the CLI version it is advised to watch the CLI setup video at 

----------

Node Monitor is written in Java (OpenJDK 11), so you'll need to have Java OpenJDK (11 or higher) installed on any machine on which you wish to run the application. If the program doesn't open, you'll probably need to install the correct java version on your machine. Oracle OpenJDK will work, but is a bit less user friendly to install, you'll need to download the JDK, extract it to your Java folder and set the environment variables for Java. 

Adoptium OpenJDK is easier to install, it installs the package automatically and sets the Java environment variables for you. Search for Adoptium OpenJDK in your internet browser and follow the installation instructions there. Make sure to install version 11 or higher.

Node Monitor needs access to the Qortal core in order to get the latest market data, which means it can only fetch new data if the Qortal core is running (preferably synced) on the same machine as Node Monitor, or on a different machine that is linked via SSH tunnel. To get the latest US dollar to QORT, Litecoin, Bitcoin and Dogecoin prices an internet connection is required.
