package org.tdar.core.bean.resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.Indexed;
import org.tdar.core.configuration.JSONTransient;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@Entity
@Indexed
@Table(name = "video")
@XStreamAlias("video")
@XmlRootElement(name = "video")
public class Video extends InformationResource {

    public Video() {
        setResourceType(ResourceType.VIDEO);
    }

    private static final long serialVersionUID = -6148366019683334981L;
    private Integer fps;
    private Integer kbps;

    @Column(name = "video_codec")
    private String videoCodec;
    @Column(name = "audio_codec")
    private String audioCodec;
    @Column(name = "audio_channels")
    private String audioChannels;
    @Column(name = "audio_kbps")
    private Integer audioKbps;
    private Integer width;
    private Integer height;
    @Column(name = "sample_frequency")
    private Integer sampleFrequency;

    public Integer getFps() {
        return fps;
    }

    public void setFps(Integer fps) {
        this.fps = fps;
    }

    public Integer getKbps() {
        return kbps;
    }

    public void setKbps(Integer kbps) {
        this.kbps = kbps;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(String audioChannels) {
        this.audioChannels = audioChannels;
    }

    public Integer getAudioKbps() {
        return audioKbps;
    }

    public void setAudioKbps(Integer audioKbps) {
        this.audioKbps = audioKbps;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getSampleFrequency() {
        return sampleFrequency;
    }

    public void setSampleFrequency(Integer sampleFrequency) {
        this.sampleFrequency = sampleFrequency;
    }

    @Override
    @Transient
    public boolean isSupportsThumbnails() {
        return true;
    }
}
