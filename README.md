<p align="center">
  <img src="https://assets-global.website-files.com/60118ca1c2eab61d24bcf151/62f2829c615e2b59c7e3879e_Full.png" alt="Corda" width="500">
</p>

# Crypto Arcade - Kotlin

Welcome to the In-Game Advertising Marketplace, where you can buy spaces within the game to advertise

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

>flow start AcceptAdvertisementProposalFlow whoAmI: EA-SPORTS, advertiser: NIKE, linearId:
