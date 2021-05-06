import scipy.stats
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

def skew_1(mean,median,sd):
    return 3*(mean-median)/sd


def skew_2(decile_9, decile_1,median):
    diff = (decile_9-median) - (median-decile_1)
    return diff / (decile_9-decile_1)


########### Blood VIT D #################
vitD_deciles = np.array([22.4,29.2,35.2,41,46.8,52.6,58.9,66.2,76.6])
vitD_mean = 48.58
vitD_sd = 21.14
vitD_med = 46.8
vitD_min = 10
vitD_max = 362

vitD_skew_1 = skew_1(vitD_mean , vitD_med,vitD_sd)
vitD_skew_2 = skew_2(vitD_deciles[-1],vitD_deciles[0],vitD_med)

vitD_x=np.linspace(0 ,131,1000)
#vitD_x=np.linspace(0,1,1000)

vitD_rv_1 = scipy.stats.skewnorm(a = vitD_skew_1, loc = vitD_mean , scale = vitD_sd)
vitD_rv_2 = scipy.stats.skewnorm(a = vitD_skew_2, loc = vitD_mean , scale = vitD_sd)
'''
plt.plot(vitD_x, vitD_rv_1.pdf(vitD_x), label='vitD skew 1')
plt.plot(vitD_x, vitD_rv_2.pdf(vitD_x), label='vitD skew 2')
plt.legend()
#plt.show()
'''
############### Vit D dietary ##################
vitD_intake_x=np.linspace(0 ,18,1000)
vitD_intake_deciles = np.array([0.36,0.7,1.02,1.35,1.75,2.21,2.88,4.06,8.81])
vitD_intake_mean = 2.89
vitD_intake_sd = 3.35
vitD_intake_med = 1.75
vitD_intake_min = 0
vitD_intake_max = 48.59
vitD_intake_skew_2 = skew_2(vitD_intake_deciles[-1],vitD_intake_deciles[0],vitD_intake_med)
vitD_intake_rv_2 = scipy.stats.skewnorm(a = vitD_intake_skew_2, loc = vitD_intake_mean , scale = vitD_intake_sd)
'''
plt.plot(vitD_intake_x, vitD_intake_rv_2.pdf(vitD_intake_x), label='vitD intake skew 2')
plt.legend()
#plt.show()
'''
############### Skin Type ##########################
skin_col = np.array([1,2,3,4,5])
skin_val = np.array([41186,358166,97640,9713,15275])
skin_val_norm = skin_val / sum(skin_val)
skin_val_rv = scipy.stats.rv_discrete(name='skin_val_rv', values=(skin_col,skin_val_norm))

R = skin_val_rv.rvs(size=100)


################ Summmer Exposure ################

sum_sun_x=np.linspace(0 , 24,1000)
sum_sun_deciles = np.array([2,2,2,3,3,4,5,6,7])
sum_sun_mean = 3.93
sum_sun_sd = 2.322
sum_sun_med = 3
sum_sun_min = 0
sum_sun_max = 24
sum_sun_skew_2 = skew_2(sum_sun_deciles[-1],sum_sun_deciles[0],sum_sun_med)
sum_sun_rv_2 = scipy.stats.skewnorm(a = sum_sun_skew_2, loc = sum_sun_mean , scale = sum_sun_sd)
'''
plt.close()
plt.plot(sum_sun_x, sum_sun_rv_2.pdf(sum_sun_x), label='sun intake skew 2')
plt.legend()
#plt.show()


'''
########### Symptoms ###############

symptom_names =['Limitation of Movement','Back Pain', 'Lower Limb Pain', 'Body Ache', 'Hip or Thigh Pain', 'Muscular Weakness', 'Scatia like']
symptom_percent = np.array([69,51,25,20,11,8,4]) 
symptom_norm = symptom_percent / sum(symptom_percent)
symptom_rv = scipy.stats.rv_discrete(name='symptom_rv', values=(np.arange(len(symptom_names)),symptom_norm))


#test_1 = symptom_rv.rvs(size=100)



vit_D_levels = np.array([8,6,4,2,0]) *2.5 #ng/ml  to nmol/L

number_of_symp = np.array([7,6,5,4,3,2,1]) 
symptom_matrix = np.array([[0,0,0,3,3,12,7],[0,0,0,0,2,3,5],[0,0,0,1,3,7,7],[0,0,1,0,2,4,4],[0,0,0,2,2,3,0]]) #number of people with number of each symptoms
symptom_matrix_norm = np.array([i/ sum(i) for i in symptom_matrix])
#print (symptom_matrix_norm)

symptom_num_rvs = [scipy.stats.rv_discrete( values=(number_of_symp,i)) for i in symptom_matrix_norm]



########### define user class and init ##################


class User:
    
    #### create random user ####### 

    def __init__(self):
        ### basic details ### 
        self.vitD = abs(vitD_rv_2.rvs())
        self.vitD_in = abs(vitD_intake_rv_2.rvs())
        self.skin_type = skin_val_rv.rvs()
        self.exposure =  sum_sun_rv_2.rvs()

    
        while self.exposure < 0:
            self.exposure =  sum_sun_rv_2.rvs()


               
        self.no_symptoms = int(np.random.exponential(scale=0.5)) # add random symptoms to population 
        #self.no_symptoms = 0
        self.symptoms = []


        ## symptom logic ###  
        for i,level in enumerate(vit_D_levels): 
            if self.vitD < level:
                self.no_symptoms = int(np.random.exponential(scale=0.5))
                #self.no_symptoms =0
                self.no_symptoms += symptom_num_rvs[i].rvs() #add symptoms based on blood level 
                
        
        
        if self.no_symptoms != 0:
            self.symptoms = list(set(symptom_rv.rvs(size=self.no_symptoms)))
        self.no_symptoms = len(self.symptoms)
        
        for i, symptom_index in enumerate(self.symptoms):
            self.symptoms[i] = symptom_names[symptom_index]


        
                

def make_n_users(n):
    users = []
    for i in range(n):
        users.append(User())
    
    return users 


        


'''

        
users_list = make_n_users(10000)

print (users_list[0].symptoms)
print (len(users_list))

df = pd.DataFrame([t.__dict__ for t in users_list ])



plt.close()
df['no_symptoms'].plot.hist(bins = len(symptom_names))
plt.show()

plt.close()
df['vitD'].plot.hist(bins = 20)

for xc in vit_D_levels :
    plt.axvline(x=xc, color='k', linestyle='--')
plt.show()

'''

