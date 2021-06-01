#!/usr/bin/env python

## will be done by importer in scala

import unittest

from dataclasses import dataclass
from typing import Dict,List

import os,re
import click

@dataclass
class Team:
    name: str

@dataclass
class Game



def parse_games(infile: str) -> List[Player]:
    players: List[Player] = []
    with open(infile) as inf:
        team = ""
        for line in inf:
            if line.startswith("#"):
                team = line.strip()[1:]
            if line.startswith("No."):
                pass
            #No.,Pos.,Player,Date of birth (age),Caps,Goals,Club
            items = line.strip().split(",")
            if len(items) > 6:
               player = Player(items[2],items[1],items[6],team)
               if len(player.name) > 3:
                    players.append(player)
    return players

players = parse_players("wiki.euro2020.squads.txt")
print(players)