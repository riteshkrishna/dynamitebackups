from django.conf.urls.defaults import *

from callOmssa import createOmssaCommand

# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()

urlpatterns = patterns('',
    ('^omssa/$',createOmssaCommand),    
)
