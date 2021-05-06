# LumaTik
A next generation health and wellbeing framework for encouraging a positive relationship with life 


## Lumatiksite 
Contains all of the API code for deploying the server either locally or in the cloud. The settings.py file is written to be deployed to either. Currently the CSS for the GCP deployment is not working, but to see how it should look follow the following:
  * Clone the repository to your local machine using `git clone 'https://github.com/clarkphd/LumaTik/'` 
  * Install the packages in the requirements.txt file using pip3 (your should be able to do `pip3 install requirements.txt`)
  * make sure you are in `./lumatiksite` and run `python3 manage.py makemigrations` then `python3 manage.py migrate`
  * To start the server run `python3 manage.py runserver`
  * Navigate to the address shown and you now have a local server running you can play with as much as you wish 

If you have datatypes that you think we should be collecting you will see the format of how we define tables and variables in `models.py`. Please put any proposed models into a new file named `models_yourname.py` to save confusion. 

## APIconnectionExample.ipynp

This contains all of the details to connect to the latest GCP deployment of the API to read and write data. Please keep all ID's below 50 when testing writing mock data as we use numbers above this for real data, other that that go crazy. Please limit POST requests to loops of less than 50. Currently there is only a single table in the database contatining the raw data however this will be upgraded soon to have a better look into the actual AI capabilities we will have. 

Please note when writing data that the DataID value is automatically populated so does not need to be sent in the PUT request.


## Planned Upgrades

There are a number of key improvements to be made to the django app and the actual data we collect. The next steps are:
  * Implementing an extra few tables and views to collect more robust data as we design the actual phone app 
  * Making the CSS work to make the actual API look pretty
  * Making a number of new views to make the API do more of the sorting and heavy lifting (It's why why use databases to start with!)
  * Connect up a dash app into the Django app for visualisation
 
 ## Useful Links
 
 * The API root URL: http://ec2-3-133-59-50.us-east-2.compute.amazonaws.com:8000/
 * Introduction to Pandas (what APIconnectionExample.ipynp currently loads the data into) : https://pandas.pydata.org/pandas-docs/stable/user_guide/10min.html 
 * Plotly Dash examples (all python source code is available for these): https://dash-gallery.plotly.host/Portal/    
 * To get started with plotly https://plotly.com/python/getting-started/ 
 * Introduction to django: https://docs.djangoproject.com/en/3.1/intro/tutorial01/ 
 * Link to colab hosted APIconnectionExample.ipynp : https://colab.research.google.com/drive/1YA0Z6uMfm3_ep9HoMndmcuL3WhzZPq8F?usp=sharing

