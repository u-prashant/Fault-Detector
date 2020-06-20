import base64
import flask
import numpy as np
from flask import jsonify
from keras import applications, Sequential
from keras.layers import Flatten, Dense, Dropout
from keras_preprocessing.image import img_to_array, load_img

app = flask.Flask(__name__)

count = 0# counter that will be used for saving images sent by the client
classes = {0: 'Defective', 1: 'Healthy'}# predicted labels decoding

def preprocess_image(filename, target_size):
    """
    :param filename: name of the file of the image that is to be processed
    :param target_size: size of the image that is to returned
    :return: processed imaged
    """
    image = load_img(filename, target_size=(512, 512))
    image = img_to_array(image)
    image = image / 255
    image = np.expand_dims(image, axis=0)
    return image

def get_imagenet_model():
    """
    loads the pretrained imagenet model
    """
    global imagenet_model
    imagenet_model = applications.VGG16(include_top=False, weights='imagenet')

def get_custom_model():
    """loads custom model"""
    global model
    model = Sequential()
    model.add(Flatten(input_shape=(16,16,512)))
    model.add(Dense(256, activation='relu'))
    model.add(Dropout(1))
    model.add(Dense(1, activation='sigmoid'))
    model.load_weights('bottleneck_fc_final_model.h5')

print(" * Loading Keras model...")
get_imagenet_model()
get_custom_model()
print(" * model successfully loaded...")


@app.route('/', methods=['GET','POST'])
def handle_request():
    imgstring = flask.request.values['image']
    imgdata = base64.b64decode(imgstring)

    # save uploaded image for data collection
    filename = './uploaded/image_' + str(count+1) + '.jpg'
    with open(filename, 'wb') as f:
        f.write(imgdata)

    image = preprocess_image(filename, target_size=(512,512))
    bottleneck_prediction = imagenet_model.predict(image)
    prediction = model.predict_classes(bottleneck_prediction)
    proba = model.predict_proba(bottleneck_prediction)

    print(prediction, proba)
    percentage = round(proba[0][0], 2)
    predicted_class = classes[prediction[0][0]]
    if predicted_class == 'Defective':
        percentage = 1 - percentage

    response = {
        'prediction': predicted_class,
        'probability': str(percentage)
    }

    print('sending response: ', response)
    return jsonify(response)