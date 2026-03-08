# Trade System README

## Overview

The **Trade System** provides Hypixel-style player-to-player trading with a safe, two-step GUI confirmation flow.

## Features

- `/trade <player>` sends a trade request (same world, within 9 blocks, expires after 60 seconds)
- `/trade accept [player]` accepts a trade request
- `/trade decline [player]` declines a trade request
- `/trade cancel` cancels an outgoing request or an active trade
- Alias: `/tr`
- Crouch + right-click a player to send a trade request (same rules as `/trade <player>`)
- Two-stage trade GUI (Offer -> Accept -> Confirm)
- Trade coins from your Skyblock profile purse alongside items (Coin Offer slots in the GUI)
- Click your Coin Offer slot and enter an amount in chat (0 to clear)
- Trade completion safety:
- Items only exchange when both players confirm
- If either player does not have enough inventory space, the trade is cancelled and items are returned
- If either player does not have enough coins for their coin offer, the trade is cancelled (no coins are removed until completion)
- On cancel/close/disconnect, offered items are returned to their owners (leftovers drop at the player's location)

## Notes

- The trade system is intended to be usable anywhere, including while visiting other players' islands (island protections still apply to world interactions).
- Coin offers trade from the currently selected Skyblock profile purse (instanced balance per profile).
- Skyblock profile switching is blocked while in an active trade to prevent cross-profile item/coin movement.
