import subprocess
with open("france.uefa2016.txt") as f:
   for line in f:
       player = line.strip()
       pre = "psql -U better -d better -h jenkins -c \"insert into players (id, name, role, club, team_id, format, image ) values(nextval('players_id_seq'), '"
       cmd = pre+player+"', '','',11 ,'','');\""
       print(cmd)
       subprocess.call(cmd, shell=True)

