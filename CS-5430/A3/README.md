# CS5430 A3

To run this project use the compiled jars in the `jars` folder. If you want to build from the source you need to add the jar in the `library` folder to your buildpath before compiling. 

## Generating Keys
To generate keys run `java -jar Gen.jar`. This will save all the neccessary keys
to `.key` files in the directory from which `Gen` is run.

## Running the Demo
To run the demo follow the following steps

1. Place the appropriate key files in the same directory as each of the programs (if you're running them all from the same directory this step can be omitted after running `Gen`)
2. Run `java -jar Bob.jar <mode>`
3. Run `java -jar Mallory.jar <mode>`
4. Run `java -jar Alice.jar <mode>`

In the above instructions `<mode>` should be replaced by one of the following:

- `plaintext`
- `MAC`
- `encryption`
- `MACencryption`

## Running on Multiple Computers
Running `Alice`, `Bob`, and `Mallory` with only a mode argument as above will run them on the default ports (`4001` for Bob, `4000` for Mallory) in loopback mode. To run them on separate computers or with different ports the full syntax is

- `java -jar Bob.jar <port> <mode>`
- `java -jar Mallory.jar <bob_hostname> <bob_port> <server_port> <mode>`
- `java -jar Alice.jar <mallory_hostname> <mallory_port> <mode>`
