# ATM Log Design

## Authors:
- Alex Clark (ayc55)
- Reuben Rappaport (rbr76)

## 1. Type and Location of Log Files
Our design will keep two types of log files, a local log of all physical events on the ATM, and a bank log of all transactions done on the ATM. This will be done to avoid letting details of customer finances be exposed if someone should steal the memory of the ATM, or otherwise get unauthorized access to it. The transaction log can be recorded by sending messages via the communication link to the bank-operated network. Communications will be encrypted to protect the confidentiality and integrity of transmitted data.

## 2. Log Entry Format
The log entries follow the following format

```
TIMESTAMP EVENT_TYPE Field1=VALUE1 Field2=VALUE2 ...
```

All log entries will be begun with a timestamp, followed by the event type. This is followed by a set of optional event specific fields in the format `Field=VALUE` which provide further details if necessary

## 3. Possible Entries

### Physical Event Log
- `TIMESTAMP POWER_ON` This event is recorded when the physical switch is used to turn the machine on
- `TIMESTAMP POWER_OFF` This event is recorded when the physical switch is used to shut down the machine (assume that the physical switch does not cut power immediately so there is time to record this)
- `TIMESTAMP CASH_CHANGE Canister=CANISTER_NUMBER Amount=AMOUNT` This event is recorded when the operator enters the amount of cash in a given canister after powering the machine on
- `TIMESTAMP LINK_DEACTIVATION` This event is recorded if the communication link to the bank ever goes inactive
- `TIMESTAMP LINK_ACTIVATION` This event is recorded when the communication link comes online. It should be logged each time that the system is powered on and also if the link goes down and comes back up again
- `TIMESTAMP CUSTOMER_SERVICE_START` This event is recorded when a customer starts a service interaction by swiping their card. It should always be matched by a closing `CUSTOMER_SERVICE_END` when the interaction finishes
- `TIMESTAMP CUSTOMER_SERVICE_END` This event is recorded when a customer service interaction ends. This could happen because they finished their transaction, because they failed too many authorization attempts, or because they timed out due to inactivity
- `TIMESTAMP READ_CARD Card_Number=CARD_NUMBER` This event is recorded when a customer swipes their card, starting a service interaction. It is logged immediately after `CUSTOMER_SERVICE_START`
- `TIMESTAMP INACTIVE_CARD Card_Number=CARD_NUMBER` This event is recorded if a customer swipes a card that is not active. It is immediately followed by `CUSTOMER_SERVICE_END`
- `TIMESTAMP PIN_ENTRY Result=SUCCESS_OR_FAILURE` This event is recorded when a customer enters their PIN and the ATM checks it with the bank. For sensitivity reasons the PINs themselves are not recorded
- `TIMESTAMP CARD_DEACTIVATION Card_Number=CARD_NUMBER` This event is recorded when a customer enters an incorrect PIN too many times and the ATM signals the bank to deactivate the card. It is immediately followed by a `CUSTOMER_SERVICE_END` event
- `TIMESTAMP SERVICE_TIMEOUT` This event is recorded when a customer service interaction times out due to the customer not entering any input for over a minute. It is immediately followed by a `CUSTOMER_SERVICE_END` event
- `TIMESTAMP SERVICE_CANCEL` This event is recorded when a customer cancels a service interaction by hitting the cancel key. It is immediately followed by a `CUSTOMER_SERVICE_END` event
- `TIMESTAMP AUTHENTICATION_SUCCESS Card_Number=CARD_NUMBER` This event is recorded when a customer successfully authenticates their card
- `TIMESTAMP WITHDRAW_FAILURE Requested_Amount=REQUESTED_AMOUNT` This event is recored when a customer attempts to withdraw more cash than the machine is capable of supplying
- `TIMESTAMP WITHDRAW_SUCCESS Canister1=CANISTER1 Amount1=AMOUNT1 Canister2=CANISTER2 Amount2=AMOUNT2 ...` This event is recorded when a customer withdraws cash from the machine. It logs how much cash was taken out of each canister
- `TIMESTAMP DEPOSIT Amount=DEPOSIT_AMOUNT` This event is recorded when a customer places cash in an envelope and deposits it into the machine

### Transaction Log
- `TIMESTAMP WRONG_PIN_ENTERED Card_Number=CARD_NUMBER` This event is recorded when a customer enters the wrong PIN at an ATM
- `TIMESTAMP ATM_AUTHENTICATED Card_Number=CARD_NUMBER` This event is recorded when a customer authenticates their card at a specific ATM
- `TIMESTAMP CARD_DEACTIVATION Card_Number=CARD_NUMBER` This event is recorded when a customer's card is deactivated due to entering the wrong pin too many times at the ATM
- `TIMESTAMP WITHDRAW_FAILURE_INSUFFICIENT_FUNDS Card_Number=CARD_NUMBER Account_Number=ACCOUNT_NUMBER Requested_Amount=REQUESTED_AMOUNT` This event is recorded when an attempted withdrawal fails because there are insufficient funds in the customer's account
- `TIMESTAMP WITHDRAW_FAILURE_ATM Card_Number=CARD_NUMBER Account_Number=ACCOUNT_NUMBER Requested_Amount=REQUESTED_AMOUNT` This event is recorded when an attempted withdrawal fails because there are insufficient funds in the ATM to supply it
- `TIMESTAMP WITHDRAW_SUCCESS Card_Number=CARD_NUMBER Account_Number=ACCOUNT_NUMBER Amount=AMOUNT` This event is recorded when a withdrawal succeeds
- `TIMESTAMP DEPOSIT Card_Number=CARD_NUMBER Account_Number=ACCOUNT_NUMBER Claimed_Amount=CLAIMED_AMOUNT` This event is recorded when a customer deposits an envelope of cash in the ATM machine
- `TIMESTAMP TRANSFER_FAILURE Card_Number=CARD_NUMBER Source_Account=SOURCE Sink_Account=SINK Amount=AMOUNT` This event is recorded when a customer attempts to transfer more cash than is available between accounts
- `TIMESTAMP TRANSFER_SUCCESS Card_Number=CARD_NUMBER Source_Account=SOURCE Sink_Account=SINK Amount=AMOUNT` This event is recorded when a customer successfully transfers cash between accounts
- `TIMESTAMP QUERY Card_Number=CARD_NUMBER Account_Number=ACCOUNT_NUMBER` This event is recorded when a customer queries the amount in one of their accounts
