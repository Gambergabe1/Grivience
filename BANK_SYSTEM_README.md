# Bank System README

## Overview

The **Bank System** adds a Skyblock-style bank account that lets players move coins between their **purse** and **bank**.

Balances are stored on the player's **currently selected Skyblock profile** (instanced balance per profile).

## Commands

- `/bank` (alias: `/bnk`) opens the bank GUI
- `/bank balance` shows purse/bank coins
- `/bank deposit <amount|all>` deposits coins from purse to bank
- `/bank withdraw <amount|all>` withdraws coins from bank to purse

## GUI

- `Deposit Coins` and `Withdraw Coins` menus
- Quick amounts: `100`, `1,000`, `10,000`, `100,000`, `1,000,000`
- `All` button to deposit/withdraw everything available
- `Custom` button: type an amount in chat (30s timeout, type `cancel` to stop)

## Notes

- Bank transfers are internal moves between purse and bank and do not count as "coins earned/spent".
- The trade system still uses **purse coins** for coin offers (Hypixel-style).

