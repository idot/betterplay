#!/usr/bin/python
# -*- coding: UTF-8 -*-

import unittest
import re

def parseFile():

def parsePlayer(line):
    nm = re.search('name=\[\[(.*)\]\]\|sort', line)
    name = nm.group(1)
    name = re.sub("\(.*\)", "", name)
    name = re.sub("\|", "", name)
    #name=[[Mahmoud Hassan (footballer)|Trézéguet]]
    sm = re.search('sortname=(\w*)\|',line)
    sortname = sm.group(1)
    pm = re.search('pos=(\w*)\|', line)
    position = pm.group(1)
    cm = re.search('club=\[\[(.*)\]\]\|club', line)
    club = cm.group(1)
    club = re.sub("\|.*","", club)
    return (name, sortname, position, club)


class TestMethods(unittest.TestCase):

    def test_parseline(self):
        line = "{{nat fs g player|no=|pos=MF|name=[[Mahmoud Hassan (footballer)|Trézéguet]]|sortname=Trezeguet|age={{Birth date and age2|df=yes|2018|6|14|1994|10|1}}|caps=22|goals=2|club=[[Kasımpaşa S.K.|Kasımpaşa]]|clubnat=TUR}}"
        #print(parsePlayer(line))
        self.assertEqual(parsePlayer(line), ("Mahmoud Hassan Trézéguet", "Trezeguet", "MF", "Kasımpaşa S.K."))


if __name__ == '__main__':
    unittest.main()
