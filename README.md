# Forward-Proxy
A Java forward proxy capable of handling HTTP traffic.

## Features
- The server is easy to set up and configure
- Support for HTTP protocols. HTTPS support is under development
- Optional IP concealment for HTTP requests
- In depth logs for ease of use and management

## Usage
Upon compiling and running the server, the user will be directed to a CLI. From here, input the commands of your choice.
Commands are in the format as follows:
- `start <port number>` : This starts the server, listening on the port of your choice. I recommend 80, for HTTP.
- `-m` : Enable HTTP masking. This conceals the connecting IP when making HTTP requests.
- `-h` : Open help menu.

An example command could be:
`-h start 80 -m`

## Connecting
Connecting to the server can be done through the browser, in Firefox, or through connection settings on the PC.
### Firefox
In Firefox, access General settings. Scroll down to network settings.
Select Manual Proxy Connection. Input the IP and open port.
### Windows
On Windows, access Settings. Select Network & internet. Select Set Up under Manual proxy setup.
Input the IP and open port.
### Linux
Open Settings. Go to Network. Next to Network Proxy, select the gear icon.
Choose Manual. Input the proxy IP and port.

## Improvements
- Currently, raw threads are used, since that's what I learned in class. A move will be made to use ExecutorPool.
- HTTPS connections currently seem to be handled incorrectly. HTTPSConnectionHandler will be updated.
- Additional configuration options will be added, including a plan to ensure website security, the safety of downloads, and check for hostile connections being made from connected machines.
