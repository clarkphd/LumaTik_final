from django.db import models
import uuid
import time
# Create your models here.






class User(models.Model):

    
    UserID = models.IntegerField(primary_key=True)
    Created = models.IntegerField(default=int(time.time()), editable=False)

    def __str__(self):

        return  str(self.UserID)



class Device(models.Model):

    DeviceID = models.IntegerField(primary_key=True) 
    Created = models.IntegerField(default=int(time.time()), editable=False)

    def __str__(self):

        return  str(self.DeviceID)



class Data(models.Model):
    ###### RAW DATA CLASS ########## 
    DataID = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    Writetime = models.IntegerField()
    Rval = models.IntegerField()
    Gval = models.IntegerField() 
    Bval = models.IntegerField()
    UVAval =models.FloatField()
    UVBval = models.FloatField()
    UVIndex = models.FloatField()
    VitDval = models.FloatField()
    UserID = models.ForeignKey(User,on_delete=models.CASCADE)
    DeviceID = models.ForeignKey(Device,on_delete=models.CASCADE)

    def __str__(self):

        return  str(self.Writetime) + str(self.VitDval)

class UserData(models.Model):

    UserDataID = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    UserID = models.ForeignKey(User,on_delete=models.CASCADE)
    DeviceID = models.ForeignKey(Device,on_delete=models.CASCADE)
    Writetime = models.IntegerField()
    Age = models.IntegerField()
    SkinPigment = models.IntegerField()
    Bedtime = models.IntegerField()
    WakeUp = models.IntegerField()
    Coverage =  models.FloatField()
    Location = models.IntegerField()

    def __str__(self):

        return str(self.Writetime)+ str( self.UserID)


class Goal(models.Model):

    GoalID = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    GoalText = models.TextField()
    GoalName = models.TextField()

    
    def __str__(self):

        return str(self.GoalID) +  str(self.GoalName)



class Wellness(models.Model):

    WellnessDataID = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    UserID = models.ForeignKey(User,on_delete=models.CASCADE)
    DeviceID = models.ForeignKey(Device,on_delete=models.CASCADE)
    Writetime = models.IntegerField()
    Feeling = models.IntegerField() 
    Headache = models.BooleanField()
    BackPain = models.BooleanField()
    NeckAche = models.BooleanField()
    Tired = models.BooleanField()
    MuslePain = models.BooleanField()
    Other = models.TextField()

    
    def __str__(self):

        return str(self.UserID) + str(self.Feeling)


class Reccomend(models.Model):


    ReccomendID = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    ReccomendText = models.TextField()
    ReccomendName = models.TextField()

    
    def __str__(self):

        return str(self.ReccomendID) +  str(self.ReccomendName)




## UVA,UVB,UVIndex,Vit D -> float 
## coverage float -> 