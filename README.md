<p align="center">
  <img src="https://assets-global.website-files.com/60118ca1c2eab61d24bcf151/62f2829c615e2b59c7e3879e_Full.png" alt="Corda" width="500">
</p>

# Crytpo Arcade - Kotlin

Welcome to the Kotlin CorDapp template. The CorDapp template is a stubbed-out CorDapp that you can use to bootstrap 
your own CorDapps.

**This is the Kotlin version of the CorDapp template.**

# Steps to run the project

1 ./gradlew clean deployNodes

Launch the CorDapp 

2 ./build/nodes/runnodes
* (Since there's an error when using a schema) 
  * ./build/nodes/runnodes --allow-hibernate-to-manage-app-schema
  * ISSUE: https://github.com/corda/corda-gradle-plugins/issues/390

# Running Flows

flow start ProposeAdvertisementFlow adType: "Banner", adPlacement: "Top", adCost: "1000 USD", adExpiry: "2023-04-21", publisher: "O=PartyA,L=London,C=GB"

run vaultQuery contractStateType: com.template.states.AdInventoryState


1st
## For the Publisher, create Publisher
flow start CreateNewAccount acctName: EA-SPORTS
flow start CreateNewAccount acctName: Rockstar-Games

flow start ShareAccountTo acctNameShared: EA-SPORTS, shareTo: advertiser
flow start ShareAccountTo acctNameShared: Rockstar-Games, shareTo: advertiser

##Share above with all the Advertisers

2nd
## For the advertisers, create advertisers
flow start CreateNewAccount acctName: NIKE
flow start CreateNewAccount acctName: PUMA

flow start ShareAccountTo acctNameShared: NIKE, shareTo: publisher
flow start ShareAccountTo acctNameShared: PUMA, shareTo: publisher

##Share above with all the publishers

flow start ProposeAdvertisementFlow whoAmI: NIKE, whereTo: EA-SPORTS, adType: "Banner", adPlacement: "Top", adCost: "1000 USD", adExpiry: "2023-04-21"

flow start ViewInboxByAccount acctname: EA-SPORTS


# Usage
