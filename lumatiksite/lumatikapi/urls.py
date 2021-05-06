# lumatikapi/urls.py



from django.urls import include, path
from rest_framework import routers
from . import views
import django_plotly_dash


router = routers.DefaultRouter()

router.register(r'User', views.UserViewSet)
router.register(r'Device', views.DeviceViewSet)
router.register(r'Reccomend',views.ReccomendViewSet)
router.register(r'Goals',views.GoalsViewSet)
router.register(r'Data', views.DataViewSet, basename='Data')
router.register(r'UserData', views.UserDataViewSet, basename='UserData')
router.register(r'Wellbeing',views.WellnessDataViewSet, basename='Wellbeing')


#router.register(r'UserDataRaw', views.SingleUserRawDataViewSet)
#router.register(r'UserDataInfo', views.SingleUserDataViewSet)

# Wire up our API using automatic URL routing.
# Additionally, we include login URLs for the browsable API.
urlpatterns = [
    path('', include(router.urls)),
    path('api-auth/', include('rest_framework.urls', namespace='rest_framework')),

]


