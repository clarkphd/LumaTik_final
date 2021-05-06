from django.shortcuts import render


from rest_framework import viewsets, filters
from django.db.models import Prefetch


# Create your views here.
from .serializers import *
from .models import * 


class DataViewSet(viewsets.ModelViewSet):

    serializer_class = DataSerializer

    def get_queryset(self): 
        if self.request.method == 'GET':
            queryset = Data.objects.all()
            uid = self.request.GET.get('q', None)
            if uid is not None:
                queryset = queryset.filter(UserID=uid)
            return queryset

    



class UserDataViewSet(viewsets.ModelViewSet):
    serializer_class = UserDataSerializer

    def get_queryset(self): 
        if self.request.method == 'GET':
            queryset = UserData.objects.all()
            uid = self.request.GET.get('q', None)
            if uid is not None:
                queryset = queryset.filter(UserID=uid)
            return queryset
  


class WellnessDataViewSet(viewsets.ModelViewSet):
    serializer_class = WellnessSerializer

    def get_queryset(self): 
        if self.request.method == 'GET':
            queryset = Wellness.objects.all()
            uid = self.request.GET.get('q', None)
            if uid is not None:
                queryset = queryset.filter(UserID=uid)
            return queryset
   
class GoalsViewSet(viewsets.ModelViewSet):
    
    queryset = Goal.objects.all().order_by('GoalID')
    serializer_class =GoalsSerializer

class ReccomendViewSet(viewsets.ModelViewSet):
    
    queryset = Reccomend.objects.all().order_by('ReccomendID')
    serializer_class = ReccomendSerializer




class UserViewSet(viewsets.ModelViewSet):
    
    queryset = User.objects.all().order_by('UserID')
    serializer_class = UserSerializer

class DeviceViewSet(viewsets.ModelViewSet):
    
    queryset = Device.objects.all().order_by('DeviceID')
    serializer_class = DeviceSerializer


'''
class DashbordView(ListView):
    template_name = 'templates\dasboard.html'
'''