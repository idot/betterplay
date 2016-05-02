import urllib2
from lxml import html
import requests


class Player:
      def __init__(self, team, countrycode, name):
         self.team = team
         self.countrycode = countrycode
         self.name = name

      def forTable(self):
          return self.team+"\t"+self.countrycode+"\t"+self.name

def getUrl(nr):
    return "http://www.uefa.com/uefaeuro/season=2016/teams/team="+str(nr)+"/squad/index.html"

tid = [2,8,13,56370,58837,39,43,47,57,58,66,63,109,110,64,113,57451,58836,122,127,128,135,57166,144]
players = []

for f in tid:
    url = getUrl(f)
    print(url)
    page = requests.get(url)
    if page.text[1:10] != "!--mobile":
       tree = html.fromstring(page.content)
       teamname = tree.xpath('//h1[@class="team-name"]/text()')
       if len(teamname) == 1:
          teamname = teamname[0]
          countrycode = tree.xpath('//span[@class="hidden-md hidden-lg"]/text()')[0]
          print(teamname+" "+countrycode)
    
       for p in tree.xpath('//div[@class="squad--stats"]//tr//td[@data-sort][1]'):
          name = p.attrib.values()[0].encode('utf-8')
          name = ' '.join(name.split())
          print(name)
          player = Player(teamname, countrycode, name)
          players.append(player)
    

f = open('teams.tab', 'w')
for p in players:
    f.write(p.forTable()+"\n")
f.close()



#   has special chars:
#   for p in tree.xpath('//div[@class="squad--stats"]//tr//a[@href][1]/text()') 
#      p.text()



