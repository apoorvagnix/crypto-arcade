<img width="400" alt="image" src="https://github.com/apoorvagnix/crypto-arcade/assets/124659748/d3638ad2-ac8a-4573-a856-dccd1678a3dd">

# Crypto Arcade - Kotlin

Welcome to the In-Game Advertising Marketplace, where you can buy spaces within the game to advertise

# Onboarding Page
<img width="1267" alt="image" src="https://github.com/apoorvagnix/crypto-arcade/assets/124659748/14e58c92-649f-46ce-b246-00e529abfeb2">

# View and Manage Propsals for Game Developers
<img width="1264" alt="image" src="https://github.com/apoorvagnix/crypto-arcade/assets/124659748/fe6397a9-32d3-4186-9c56-8831b91b4f40">

# Propose advertisment for Advertisers
<img width="1211" alt="image" src="https://github.com/apoorvagnix/crypto-arcade/assets/124659748/75ce149b-801d-4130-bc12-e6f8279e0aad">


# Steps to run the project

Build the application
> ./gradlew clean deployNodes

Launch the CorDapp now

> ./build/nodes/runnodes

> ./build/nodes/runnodes --allow-hibernate-to-manage-app-schema
* (Since there's an error when using a schema) 
  ISSUE: https://github.com/corda/corda-gradle-plugins/issues/390 *

# Running Flows

run vaultQuery contractStateType: com.template.states.AdInventoryState



## For the Publisher, create Publisher

>flow start CreateNewAccount acctName: EA-SPORTS
> 
>flow start CreateNewAccount acctName: Rockstar-Games

> flow start ShareAccountTo acctNameShared: EA-SPORTS, shareTo: advertiser
> 
> flow start ShareAccountTo acctNameShared: Rockstar-Games, shareTo: advertiser

**ShareAccountTo** will share the Publishers accountInfo to all the advertisers


## For the advertisers, create advertisers

>flow start CreateNewAccount acctName: NIKE
> 
>flow start CreateNewAccount acctName: PUMA

>flow start ShareAccountTo acctNameShared: NIKE, shareTo: publisher
> 
>flow start ShareAccountTo acctNameShared: PUMA, shareTo: publisher

**ShareAccountTo** will share the Advertisers accountInfo to all the publishers


## The Advertiser proposes an advertisment to the publisher

>flow start ProposeAdvertisementFlow whoAmI: NIKE, whereTo: EA-SPORTS, adType: "Banner", adPlacement: "Top", adCost: "1000 USD", adExpiry: "2023-04-21"


>flow start ViewInboxByAccount acctname: EA-SPORTS
- Use ViewInboxByAccount to view any proposal made by advertisers -> for publishers
- Use ViewInboxByAccount to view any offers accepted by publishers -> for advertisers

>flow start AcceptAdvertisementProposalFlow publisher: EA-SPORTS, advertiser: NIKE, linearId:
