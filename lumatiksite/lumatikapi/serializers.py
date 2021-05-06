from rest_framework import serializers

from .models import *


class DataSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Data
        fields = ('DataID','Writetime','Rval','Gval','Bval','UVAval','UVBval','UVIndex','VitDval','UserID','DeviceID')


class UserDataSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = UserData
        fields = ('UserDataID','UserID','DeviceID','Writetime','Age','SkinPigment','Bedtime','WakeUp','Coverage','Location')


class UserSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = User
        fields = ('UserID','Created')


class DeviceSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Device
        fields = ('DeviceID','Created')

class WellnessSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Wellness
        fields = ('WellnessDataID','UserID','DeviceID', 'Writetime','Feeling','Headache','BackPain','NeckAche','Tired','MuslePain','Other')



class GoalsSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Goal
        fields = ('__all__')


class ReccomendSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Reccomend
        fields = ('__all__')


