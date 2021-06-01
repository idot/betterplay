import time
from locust import HttpUser,TaskSet,task,between
import logging,sys

MAXUSER=99
Users = [(f"n{nr}",f"p{nr}") for nr in range(1,MAXUSER)]

class UserBehaviour(TaskSet):
    self.prefix = "/fifa2018"
    self.game_min = 1
    self.game_max = 36

    def on_start(self):
        self.username="no_user"
        self.password="no_pass"
        if len(Users) > 0:
            self.username, self.password = Users.pop()
        #self.client.post("/login",data={"username": self.username,"password": self.password})
        logging.info('Login with %s email and %s password', self.userName, self.password)       
    
    def random_game(self):
        return random.randint(self.game_min, self.game_max)

    def random_user(self):
        return random.


    @task(4)
    def get_bets(self):
        logging.info(f"get bets {self.username}")

    @task(4)
    def get_games(self):
        self.client.get(f"/{self.prefix}/games")

    @task(3)
    def get_bets_for_game(self):
        nr = randint(self.game_min, self.game_max)
        self.client.get(f"/{self.prefix}/game/{nr}/bets")

    @task(4)
    def get_users(self):
        self.client.get(f"/{self.prefix}/users")

    @task(2)
    def get_special_bets(self):
        self.client.get(f"/{self.prefix}/user/{self.username}/special")

    

    @task(8)
    def set_bet(self):
        for item_id in range(10):
            #self.client.get(f"/item?id={item_id}", name="/item")
            logging.info(f"set bet {self.username} {item_id}")
            time.sleep(1)


class User(HttpUser):
    tasks= [UserBehaviour]
    wait_time = between(5, 10)
    host="http://idot.at" 




