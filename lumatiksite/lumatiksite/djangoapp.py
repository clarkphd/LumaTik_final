import dash
import dash_core_components as dcc
import dash_html_components as html
from dash.dependencies import Input, Output
import plotly.express as px
import json 
import requests
import plotly.graph_objects as go
import numpy as np
import time 
import pandas as pd

from django_plotly_dash import DjangoDash


#df = pd.read_csv('https://raw.githubusercontent.com/plotly/datasets/master/gapminderDataFiveYear.csv')

external_stylesheets = ['https://codepen.io/chriddyp/pen/bWLwgP.css']

#app = dash.Dash(__name__, external_stylesheets=external_stylesheets)

app = DjangoDash('Dashboard_plotly', external_stylesheets=external_stylesheets)


base_url = "http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/"

links = json.loads(requests.get(base_url).content)


uk_skin = np.array([41186,358166,97640,9713,15275])
dropdown_opts = ["Rval","Gval","Bval","UVAval","UVBval","UVIndex","VitDval"]

uk_skin = (uk_skin / sum(uk_skin))*100

##### Get Active users ###### 


def get_no_users():
    users = json.loads(requests.get(links['User']).content)

    return len(users)


def get_user_unique_df():

    user_dat = pd.read_json(requests.get(links['UserData']).content) 
    user_dat = user_dat.drop_duplicates(subset=['UserID'])
    return user_dat


def plot_skin_type(user_df):
    user_df = user_df.loc[user_df['SkinPigment'] <6 ]
    fig = px.histogram(user_df, x='SkinPigment', nbins = 5 , histnorm='percent', opacity=0.8)
    fig.add_bar(x = np.arange(1,6),y = uk_skin, opacity=0.5, name = 'UK')
    fig.update_layout(barmode='overlay')
    return fig


##### get raw data #######

def get_raw_data():

    raw_dat = pd.read_json(requests.get(links['Data']).content) 
    raw_dat = raw_dat.loc[raw_dat['Writetime'] > (int(time.time())- (2*604800)) ]
    raw_dat['Writetime'] = pd.to_datetime(raw_dat['Writetime'], unit='s')
    return raw_dat

def plot_raw(option):

    fig = px.scatter(get_raw_data(),x = 'Writetime',y= option)
    return fig






def serve_layout():
    return (
   html.Div([
    html.H1('There are {} active users'.format(get_no_users())),
    html.Div([
    dcc.Graph(id = "pigment_hist", figure = plot_skin_type(get_user_unique_df())),
    dcc.Dropdown(id = 'raw_drop', options = [{'label': i, 'value': i}for i in dropdown_opts], value = "UVIndex"),
    dcc.Graph(id = 'Raw_Graph')],style={ 'height': 100, 'marginTop': 25})
   ],style={ 'height': 100, 'marginTop': 25})
    
    )

app.layout =  serve_layout


@app.callback(
    Output('Raw_Graph', 'figure'),
    Input('raw_drop', 'value'))
def update_raw_graph(raw_drop):
    return plot_raw(raw_drop)



#if __name__ == '__main__':
#    app.run_server(debug=True)

