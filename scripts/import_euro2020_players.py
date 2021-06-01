#!/usr/bin/env python

import unittest

from dataclasses import dataclass
from typing import Dict,List

import os,re
import click

@dataclass
class Player:
    name: str
    role: str
    club: str
    team: str

    def to_line(self):
        return "\t".join([self.name, self.role, self.team, self.club])

def parse_players(infile: str) -> List[Player]:
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
with open("../conf/data/euro2020_players.tab", "w") as outf:
    for p in players:
        outf.write(p.to_line()+"\n")