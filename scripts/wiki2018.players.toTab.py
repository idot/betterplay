#!/usr/bin/python
# -*- coding: UTF-8 -*-

import unittest
import re
import sys

START="{{nat fs g start}}\n"
END="{{nat fs g end}}\n"
END2="{{nat fs end}}\n"
BREAK="{{nat fs break}}\n"

def parseCountry(infile):
    players = []
    table = False
    for line in infile:
        if line == END or line == END2:
           return players
        #print line
        #print(line == START, START, line)
        if line == START:
           #print(line)
           table = True
           continue
        if line == BREAK:
           continue
        if table:
           player = parsePlayer(line)
           players.append(player)


def parseFile():
    countryPlayers = {}
    with open("wiki.fifa2018.squads.txt") as infile:
         for line in infile:
             m = re.search("===(\w*)===", line)
        #     print(m, line)
             if m is not None:
                country = m.group(1)
                players = parseCountry(infile)
                countryPlayers[country] = players
                continue
    return countryPlayers

def parsePlayer(line):
    try:
        nm = re.search('name=\[\[(.*)\]\]\|sort', line)
        name = nm.group(1)
        name = re.sub("\(.*\)", "", name)
        name = re.sub("\|", "", name)
        #name=[[Mahmoud Hassan (footballer)|Trézéguet]]
        sm = re.search('sortname=([a-zA-Z, _]*)',line)
        sortname = sm.group(1)
        pm = re.search('pos=(\w*)\|', line)
        position = pm.group(1)
        cm = re.search('club=\[\[(.*)\]\]\|club', line)
        club = cm.group(1)
        club = re.sub("\|.*","", club)
        return (name, sortname, position, club)
    except Exception as err:
        print line
        raise err

def convert():
    pC = parseFile()
    with open("wiki.fifa2018.squads.tab", "w") as outf:
        outf.write("country\tname\tsortname\tposition\tclub\n")
        for country, players in pC.iteritems():
            for p in players:
                line = country+"\t"+"\t".join(p)+"\n"
                outf.write(line)
    #print(pC)

class TestMethods(unittest.TestCase):

    def test_parseline(self):
        line = "{{nat fs g player|no=|pos=MF|name=[[Mahmoud Hassan (footballer)|Trézéguet]]|sortname=Trezeguet|age={{Birth date and age2|df=yes|2018|6|14|1994|10|1}}|caps=22|goals=2|club=[[Kasımpaşa S.K.|Kasımpaşa]]|clubnat=TUR}}"
        #print(parsePlayer(line))
        self.assertEqual(parsePlayer(line), ("Mahmoud Hassan Trézéguet", "Trezeguet", "MF", "Kasımpaşa S.K."))


if __name__ == '__main__':
    #unittest.main()
    convert()
