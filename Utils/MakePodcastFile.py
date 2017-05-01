# Makes podcast.xml and image files based on the list of RSS feed urls in urls.txt
# Warning: no check for images with equal names

import requests
import uuid
import posixpath

from xml.etree import ElementTree as ET
from PIL import Image # pip3 install Pillow

def indent(elem, level=0):
    i = "\n" + level*"    "
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + "    "
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
        for elem in elem:
            indent(elem, level+1)
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = i

podcasts_element = ET.Element("podcasts")

itunes_ns = {"itunes": "http://www.itunes.com/dtds/podcast-1.0.dtd"}
original_image_prefix = "original_"

# Android drawable names must start from a letter and be in a lower case
pod_image_prefix = "pod_"

url_file = open("urls.txt")

for url in url_file:
    # Skip commented lines
    if url.startswith(";"):
        continue
    url = url.rstrip()

    print("---------------------------------------------------------")
    print(url)
    try:
        resp = requests.get(url)
        if resp.status_code == 200:
            rss = ET.fromstring(resp.text)
            for channel in rss.findall("channel"):
                podcast_element = ET.SubElement(podcasts_element, "podcast")
                code = uuid.uuid4()
                ET.SubElement(podcast_element, "code").text = str(code)
                ET.SubElement(podcast_element, "country").text = "UK"
                ET.SubElement(podcast_element, "level").text = "INTERMEDIATE"
                ET.SubElement(podcast_element, "title").text = channel.find("title").text
                ET.SubElement(podcast_element, "description").text = channel.find("description").text
                ET.SubElement(podcast_element, "rss_url").text = url

                image_url = channel.find("itunes:image", itunes_ns).get("href")
                image_name = posixpath.basename(image_url).lower().replace("-", "_")
                image_resp = requests.get(image_url)
                if image_resp.status_code == 200:
                    image_file = open(original_image_prefix + image_name, "wb")
                    image_file.write(image_resp.content)
                    image_file.close()

                    image = Image.open(original_image_prefix + image_name)
                    image = image.resize((400, 400), Image.ANTIALIAS)
                    image.save(pod_image_prefix + image_name)

                    ET.SubElement(podcast_element, "image_src").text = pod_image_prefix + image_name
                else:
                    print("image retrieval status code: " + str(resp.status_code))
        else:
            print("status code: " + str(resp.status_code))
    except Exception as e:
        print(e)

url_file.close()

podcasts_tree = ET.ElementTree(podcasts_element)
indent(podcasts_element)
podcasts_tree.write("podcasts.xml")

